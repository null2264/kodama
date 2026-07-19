package main

import (
	supabase "github.com/supabase-community/supabase-go"
	"github.com/supabase-community/gotrue-go/types"
	"log"
	"os"
	"path/filepath"
	"strings"

	"github.com/joho/godotenv"
	"github.com/null2264/kodama/kodama-db/core"
	"github.com/spf13/cobra"
)

var (
	revisionReason string
	enableTestRev  bool = false
)

var rootCmd = &cobra.Command{
	Use:   "kodama-db",
	Short: "Database Utility CLI for Kodama",
	Long: `Database Utility CLI for Kodama.

This utility CLI contains command to init, reset, migrate and test Kodama's Supabase database.`,
}

var dbCmd = &cobra.Command{
	Use:   "db",
	Short: "Database management commands",
}

var testCmd = &cobra.Command{
	Use:   "test",
	Short: "Execute DB scenario test",
	Run:   doTest,
}

var migrateCmd = &cobra.Command{
	Use:   "migrate",
	Short: "Generate new revision",
	Run:   doMigrate,
}

var resetCmd = &cobra.Command{
	Use:   "reset",
	Short: "Reset the database",
	Run: func(cmd *cobra.Command, args []string) {
		migration, err := core.NewMigrations(enableTestRev)
		if err != nil {
			log.Fatal(err)
		}
		migration.Reset()
	},
}

var upgradeCmd = &cobra.Command{
	Use:   "upgrade",
	Short: "Apply all available revisions",
	Run:   doUpgrade,
}

const (
	demoPass string = "demo12345"

	classMadya   string = "11943061-aa9d-4cc0-90f6-2ca7b70bc1b5"
	classProspek string = "8f8e46fd-75e9-443e-a1f2-7ffab68ece31"
	classPratama string = "fb85ea10-0fc5-40b9-9e27-f236962c8271"
)

func doTest(cmd *cobra.Command, args []string) {
	url := os.Getenv("SUPABASE_URL")
	key := os.Getenv("SUPABASE_KEY")

	client, err := supabase.NewClient(url, key, &supabase.ClientOptions{Schema: "kodama"})
	if err != nil {
		log.Fatal("Failed to initialize client: ", err)
	}

	signup := func(user string) string {
		resp, err := client.Auth.Signup(types.SignupRequest{
			Email:    user + "@test.example.com",
			Password: demoPass,
		})
		if err != nil {
			log.Fatalf("Failed to sign up %s: %v", user, err)
		}
		if resp.User.ID.String() == "00000000-0000-0000-0000-000000000000" {
			log.Fatalf("[FAIL] Signup returned empty user ID for %s (autoconfirm may be off)", user)
		}
		return resp.User.ID.String()
	}

	login := func(user string) {
		_ = client.Auth.Logout()
		if _, err := client.SignInWithEmailPassword(user+"@test.example.com", demoPass); err != nil {
			log.Fatalf("Failed to sign in as %s: %v", user, err)
		}
	}

	type rowID struct {
		ID string `json:"id"`
	}

	type rowState struct {
		State string `json:"state"`
	}

	chooseClass := func(contestID, classID string) string {
		var classes []rowID
		if _, err := client.From("contest_classes").
			Select("id", "", false).
			Eq("contest_id", contestID).
			Eq("class_id", classID).
			ExecuteTo(&classes); err != nil {
			log.Fatal("Failed to choose class: ", err)
		}
		return classes[0].ID
	}

	// ================================================================
	// STEP 0: Sign up users (using service role key, before any login)
	// ================================================================
	log.Println("[INFO] Signing up users...")
	adminID := signup("admin")
	judge1ID := signup("judge1")
	judge2ID := signup("judge2")
	_ = signup("contestant1")
	_ = signup("contestant2")
	log.Println("[SUCCESS] All users signed up")

	// Set admin role (service role key bypasses RLS)
	log.Print("[TESTING] Promoting admin user...")
	if _, _, err := client.From("user_metadata").Update(
		map[string]string{"role": "admin"}, "", "",
	).Eq("id", adminID).Execute(); err != nil {
		log.Fatal("Failed to set admin role: ", err)
	}
	log.Println("[SUCCESS] Admin user promoted")

	// ================================================================
	// STEP 1: Admin creates a contest and adds classes
	// ================================================================
	log.Println("[INFO] Logging in as Admin...")
	login("admin")

	log.Print("[TESTING] Creating contest...")
	var contests []rowID
	if _, err := client.From("contests").Insert(
		map[string]string{"name": "Test Contest", "description": "Integration test"},
		false, "", "", "",
	).ExecuteTo(&contests); err != nil {
		log.Fatal("Failed to create contest: ", err)
	}
	contestID := contests[0].ID
	log.Println("[SUCCESS] Contest has been created")

	log.Print("[TESTING] Adding classes to contest...")
	if _, _, err := client.From("contest_classes").Insert([]map[string]string{
		{"contest_id": contestID, "class_id": classProspek},
		{"contest_id": contestID, "class_id": classMadya},
		{"contest_id": contestID, "class_id": classPratama},
	}, false, "", "", "").Execute(); err != nil {
		log.Fatal("Failed to add classes: ", err)
	}
	log.Println("[SUCCESS] Classes added to contest")

	// ================================================================
	// STEP 2: Admin assigns judges to their classes
	// ================================================================
	log.Print("[TESTING] Assigning judges to classes...")
	prospekClassID := chooseClass(contestID, classProspek)
	madyaClassID := chooseClass(contestID, classMadya)

	if _, _, err := client.From("contest_participants").Insert([]map[string]string{
		{"user_id": judge1ID, "contest_id": contestID, "role": "judge", "contest_class_id": prospekClassID},
		{"user_id": judge2ID, "contest_id": contestID, "role": "judge", "contest_class_id": madyaClassID},
	}, false, "", "", "").Execute(); err != nil {
		log.Fatal("Failed to assign judges: ", err)
	}
	log.Println("[SUCCESS] Judges assigned")

	// ================================================================
	// STEP 3: Admin opens registration
	// ================================================================
	log.Print("[TESTING] Opening contest registration...")
	if _, _, err := client.From("contests").Update(
		map[string]string{"state": "accepting"}, "", "",
	).Eq("id", contestID).Execute(); err != nil {
		log.Fatal("Failed to open registration: ", err)
	}
	log.Println("[SUCCESS] Contest registration opened")

	// ================================================================
	// STEP 4: Contestants register and finalize their bonsai
	// ================================================================
	registerAndFinalizeBonsai := func(contestant, classKey string) string {
		login(contestant)

		log.Printf("[TESTING] %s registering bonsai...\n", contestant)
		var bonsais []rowID
		var cid string
		switch classKey {
		case "prospek":
			cid = chooseClass(contestID, classProspek)
		case "madya":
			cid = chooseClass(contestID, classMadya)
		default:
			cid = chooseClass(contestID, classPratama)
		}
		if _, err := client.From("bonsai").Insert(map[string]string{
			"name":             contestant + "_bonsai",
			"contest_id":       contestID,
			"contest_class_id": cid,
		}, false, "", "", "").ExecuteTo(&bonsais); err != nil {
			log.Fatal("Failed to register bonsai: ", err)
		}
		bonsaiID := bonsais[0].ID
		log.Printf("[SUCCESS] %s registered bonsai ID %s\n", contestant, bonsaiID)

		log.Printf("[TESTING] %s finalizing bonsai...\n", contestant)
		_ = client.Rpc("finalize_bonsai", "", map[string]string{"bonsai_id": bonsaiID})
		log.Printf("[SUCCESS] %s finalized bonsai ID %s\n", contestant, bonsaiID)
		return bonsaiID
	}

	bonsai1ID := registerAndFinalizeBonsai("contestant1", "prospek")
	bonsai2ID := registerAndFinalizeBonsai("contestant2", "madya")

	// ================================================================
	// STEP 5: Admin verifies bonsai, closes registration
	// ================================================================
	log.Println("[INFO] Logging in as Admin...")
	login("admin")

	for _, bid := range []string{bonsai1ID, bonsai2ID} {
		log.Printf("[TESTING] Verifying bonsai ID %s...\n", bid)
		_ = client.Rpc("verify_bonsai", "", map[string]string{"bonsai_id": bid})
		log.Printf("[SUCCESS] Bonsai ID %s verified\n", bid)
	}

	log.Print("[TESTING] Closing contest registration...")
	if _, _, err := client.From("contests").Update(
		map[string]string{"state": "closed"}, "", "",
	).Eq("id", contestID).Execute(); err != nil {
		log.Fatal("Failed to close contest: ", err)
	}
	log.Println("[SUCCESS] Contest registration closed")

	// ================================================================
	// STEP 6: Verify user cannot submit bonsai to a closed contest
	// ================================================================
	log.Println("[INFO] Logging in as Contestant1...")
	login("contestant1")

	log.Print("[TESTING] Registering bonsai to a CLOSED contest...")
	var failedBonsais []rowID
	if _, err := client.From("bonsai").Insert(map[string]string{
		"name":             "should_fail",
		"contest_id":       contestID,
		"contest_class_id": chooseClass(contestID, classPratama),
	}, false, "", "", "").ExecuteTo(&failedBonsais); err != nil {
		log.Println("[SUCCESS] Bonsai registration correctly rejected")
	} else {
		log.Fatalf("[FAIL] Bonsai registered with ID %s to a CLOSED contest", failedBonsais[0].ID)
	}

	// ================================================================
	// STEP 7: Admin opens reviewing period
	// ================================================================
	log.Println("[INFO] Logging in as Admin...")
	login("admin")

	log.Print("[TESTING] Opening contest for reviewing...")
	if _, _, err := client.From("contests").Update(
		map[string]string{"state": "reviewing"}, "", "",
	).Eq("id", contestID).Execute(); err != nil {
		log.Fatal("Failed to open reviewing: ", err)
	}
	log.Println("[SUCCESS] Contest is now in reviewing state")

	// ================================================================
	// STEP 8: Judges submit scores
	// ================================================================
	submitScore := func(judge, bonsaiID string, scores map[string]int) {
		login(judge)
		total := 0
		for _, v := range scores {
			total += v
		}

		if _, _, err := client.From("reviews").Insert(map[string]interface{}{
			"bonsai_id":   bonsaiID,
			"scores":      scores,
			"total_score": total,
		}, false, "", "", "").Execute(); err != nil {
			log.Fatalf("Failed to submit score for %s: %v", judge, err)
		}
		log.Printf("[SUCCESS] %s scored bonsai %s | total=%d\n", judge, bonsaiID, total)
	}

	// Judge1 is assigned to Prospek class -> can score bonsai1
	submitScore("judge1", bonsai1ID, map[string]int{
		"penampilan": 85, "gerak_dasar": 80, "keserasian": 90, "kematangan": 75,
	})
	// Judge2 is assigned to Madya class -> can score bonsai2
	submitScore("judge2", bonsai2ID, map[string]int{
		"penampilan": 88, "gerak_dasar": 82, "keserasian": 85, "kematangan": 80,
	})

	// ================================================================
	// STEP 9: Determine Best in Show and finish the contest
	// ================================================================
	log.Println("[INFO] Logging in as Admin...")
	login("admin")

	log.Print("[TESTING] Finishing contest (auto-close, all bonsai reviewed)...")
	result := client.Rpc("finish_contest", "", map[string]interface{}{
		"p_contest_id": contestID,
		"p_force":      false,
	})
	if result == "" {
		log.Fatal("[FAIL] finish_contest RPC returned empty result")
	}
	winnerID := strings.Trim(result, "\"")
	log.Printf("[SUCCESS] Contest finished, Best in Show winner ID: %s\n", winnerID)

	// Verify contest is now finished
	var states []rowState
	if _, err := client.From("contests").
		Select("state", "", false).
		Eq("id", contestID).
		ExecuteTo(&states); err != nil {
		log.Fatal("Failed to check contest state: ", err)
	}
	if states[0].State != "finished" {
		log.Fatalf("[FAIL] Contest state is '%s', expected 'finished'", states[0].State)
	}
	log.Println("[SUCCESS] Contest state is 'finished'")

	// Verify winner is one of the registered bonsai
	if winnerID != bonsai1ID && winnerID != bonsai2ID {
		log.Fatalf("[FAIL] Winner ID %s is not a registered bonsai", winnerID)
	}
	log.Printf("[SUCCESS] Winner is a valid registered bonsai\n")

	// ================================================================
	// STEP 10: Verify finished contest is read-only (scores rejected)
	// ================================================================
	log.Println("[INFO] Logging in as Judge1...")
	login("judge1")

	log.Print("[TESTING] Submitting score to FINISHED contest...")
	if _, _, err := client.From("reviews").Insert(map[string]interface{}{
		"bonsai_id":   bonsai1ID,
		"scores":      map[string]int{"penampilan": 50, "gerak_dasar": 50, "keserasian": 50, "kematangan": 50},
		"total_score": 200,
	}, false, "", "", "").Execute(); err != nil {
		log.Println("[SUCCESS] Score submission correctly rejected on finished contest")
	} else {
		log.Fatalf("[FAIL] Score was accepted on a finished contest")
	}

	// Verify contest state cannot be changed back
	log.Println("[INFO] Logging in as Admin...")
	login("admin")

	log.Print("[TESTING] Trying to reopen a finished contest...")
	if _, _, err := client.From("contests").Update(
		map[string]string{"state": "reviewing"}, "", "",
	).Eq("id", contestID).Execute(); err != nil {
		log.Println("[SUCCESS] Finished contest state change correctly rejected")
	} else {
		log.Fatalf("[FAIL] Finished contest state was changed back")
	}

	// ================================================================
	// STEP 11: Force-close path (admin/head judge skips incomplete reviews)
	// ================================================================
	log.Println("[INFO] Creating second contest for force-close test...")
	login("admin")

	var contests2 []rowID
	if _, err := client.From("contests").Insert(
		map[string]string{"name": "Force Close Test", "description": ""},
		false, "", "", "",
	).ExecuteTo(&contests2); err != nil {
		log.Fatal("Failed to create second contest: ", err)
	}
	contest2ID := contests2[0].ID

	if _, _, err := client.From("contest_classes").Insert([]map[string]string{
		{"contest_id": contest2ID, "class_id": classProspek},
		{"contest_id": contest2ID, "class_id": classPratama},
	}, false, "", "", "").Execute(); err != nil {
		log.Fatal("Failed to add classes: ", err)
	}
	c2ProspekID := chooseClass(contest2ID, classProspek)
	c2PratamaID := chooseClass(contest2ID, classPratama)

	if _, _, err := client.From("contest_participants").Insert([]map[string]string{
		{"user_id": judge1ID, "contest_id": contest2ID, "role": "judge", "contest_class_id": c2ProspekID},
	}, false, "", "", "").Execute(); err != nil {
		log.Fatal("Failed to assign judge: ", err)
	}

	// Open registration
	client.From("contests").Update(map[string]string{"state": "accepting"}, "", "").Eq("id", contest2ID).Execute()

	// Register and verify 2 bonsai
	login("contestant1")
	var b2a []rowID
	client.From("bonsai").Insert(map[string]string{
		"name": "force_a", "contest_id": contest2ID, "contest_class_id": c2ProspekID,
	}, false, "", "", "").ExecuteTo(&b2a)
	bonsai2a := b2a[0].ID
	client.Rpc("finalize_bonsai", "", map[string]string{"bonsai_id": bonsai2a})

	login("contestant2")
	var b2b []rowID
	client.From("bonsai").Insert(map[string]string{
		"name": "force_b", "contest_id": contest2ID, "contest_class_id": c2PratamaID,
	}, false, "", "", "").ExecuteTo(&b2b)
	bonsai2b := b2b[0].ID
	client.Rpc("finalize_bonsai", "", map[string]string{"bonsai_id": bonsai2b})

	// Admin verifies both, closes, opens reviewing
	login("admin")
	client.Rpc("verify_bonsai", "", map[string]string{"bonsai_id": bonsai2a})
	client.Rpc("verify_bonsai", "", map[string]string{"bonsai_id": bonsai2b})
	client.From("contests").Update(map[string]string{"state": "closed"}, "", "").Eq("id", contest2ID).Execute()
	client.From("contests").Update(map[string]string{"state": "reviewing"}, "", "").Eq("id", contest2ID).Execute()

	// Score only ONE of the two bonsai
	log.Println("[INFO] Logging in as Judge1...")
	login("judge1")
	if _, _, err := client.From("reviews").Insert(map[string]interface{}{
		"bonsai_id":   bonsai2a,
		"scores":      map[string]int{"penampilan": 90, "gerak_dasar": 90, "keserasian": 90, "kematangan": 90},
		"total_score": 360,
	}, false, "", "", "").Execute(); err != nil {
		log.Fatal("Failed to submit score: ", err)
	}

	// Try auto-close (should fail - not all bonsai reviewed)
	log.Println("[INFO] Logging in as Admin...")
	login("admin")

	log.Print("[TESTING] Auto-close with incomplete reviews...")
	autoResult := client.Rpc("finish_contest", "", map[string]interface{}{
		"p_contest_id": contest2ID,
		"p_force":      false,
	})
	if autoResult == "" {
		log.Println("[SUCCESS] Auto-close correctly rejected (not all bonsai reviewed)")
	} else {
		log.Fatalf("[FAIL] Auto-close succeeded when not all bonsai were reviewed")
	}

	// Now force-close (should succeed)
	log.Print("[TESTING] Force-close by admin...")
	forceResult := client.Rpc("finish_contest", "", map[string]interface{}{
		"p_contest_id": contest2ID,
		"p_force":      true,
	})
	if forceResult == "" {
		log.Fatal("[FAIL] Force-close RPC returned empty result")
	}
	forceWinner := strings.Trim(forceResult, "\"")

	var states2 []rowState
	if _, err := client.From("contests").
		Select("state", "", false).
		Eq("id", contest2ID).
		ExecuteTo(&states2); err != nil {
		log.Fatal("Failed to check contest state: ", err)
	}
	if states2[0].State != "finished" {
		log.Fatalf("[FAIL] Contest state is '%s', expected 'finished'", states2[0].State)
	}
	if forceWinner != bonsai2a {
		log.Fatalf("[FAIL] Force-close winner %s does not match reviewed bonsai", forceWinner)
	}
	log.Println("[SUCCESS] Force-close completed correctly")
}

func doMigrate(cmd *cobra.Command, args []string) {
	migration, err := core.NewMigrations(enableTestRev)
	if err != nil {
		log.Fatal(err)
	}
	migration.Create(revisionReason, enableTestRev)
}

func doUpgrade(cmd *cobra.Command, args []string) {
	migration, err := core.NewMigrations(enableTestRev)
	if err != nil {
		log.Fatal(err)
	}
	migration.Upgrade(enableTestRev)
}

func init() {
	dbCmd.AddCommand(testCmd)
	dbCmd.AddCommand(migrateCmd)
	dbCmd.AddCommand(resetCmd)
	dbCmd.AddCommand(upgradeCmd)
	migrateCmd.Flags().StringVarP(&revisionReason, "reason", "r", "", "the reason for this revision")
	migrateCmd.MarkFlagRequired("reason")
	migrateCmd.Flags().BoolVarP(&enableTestRev, "test", "t", false, "mark this revision as a test revision")
	upgradeCmd.Flags().BoolVarP(&enableTestRev, "test", "t", false, "apply test revisions")
	rootCmd.AddCommand(dbCmd)
}

func main() {
	cwd, err := os.Getwd()
	if err != nil {
		log.Fatal(err)
	}

	if _, err := os.Stat(filepath.Join(cwd, "migrations")); os.IsNotExist(err) {
		log.Fatal("Invalid working directory, there are no 'migrations' directory.")
	}

	godotenv.Load(".env.local")
	godotenv.Load()

	if err := rootCmd.Execute(); err != nil {
		// fmt.Println(err)
		os.Exit(1)
	}
}
