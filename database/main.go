package main

import (
	supabase "github.com/supabase-community/supabase-go"
	"log"
	"os"
	"path/filepath"

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

	login := func(user string) {
		_ = client.Auth.Logout()
		if _, err := client.SignInWithEmailPassword(user+"@test.example.com", demoPass); err != nil {
			log.Fatalf("Failed to sign in as %s: %v", user, err)
		}
	}

	type rowID struct {
		ID string `json:"id"`
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

	// --- Admin creates a contest and finalizes it
	log.Println("[INFO] Logging in as Admin...")
	login("admin")

	log.Print("[TESTING] Creating contest...")
	var contests []rowID
	if _, err := client.From("contests").Insert(
		map[string]string{"name": "Test", "description": "Lorem ipsum"},
		false, "", "", "",
	).ExecuteTo(&contests); err != nil {
		log.Fatal("Failed to create contest: ", err)
	}
	contestID := contests[0].ID
	log.Println("[SUCCESS] Contest has been created")

	log.Print("[TESTING] Adding classes to contest draft...")
	if _, _, err := client.From("contest_classes").Insert([]map[string]string{
		{"contest_id": contestID, "class_id": classProspek},
		{"contest_id": contestID, "class_id": classMadya},
		{"contest_id": contestID, "class_id": classPratama},
	}, false, "", "", "").Execute(); err != nil {
		log.Fatal("Failed to add classes: ", err)
	}
	log.Println("[SUCCESS] Classes added to contest draft")

	log.Print("[TESTING] Finalizing contest...")
	if _, _, err := client.From("contests").Update(
		map[string]string{"state": "accepting"}, "", "",
	).Eq("id", contestID).Execute(); err != nil {
		log.Fatal("Failed to finalize contest: ", err)
	}
	log.Println("[SUCCESS] Contest has been finalized")

	// --- Contestants registering their bonsai
	log.Println("[INFO] Logging in as Demo...")
	login("demo")

	registerAndFinalizeBonsai := func() string {
		log.Print("[TESTING] Registering bonsai to a contest as a contestant...")
		var bonsais []rowID
		if _, err := client.From("bonsai").Insert(map[string]string{
			"name":             "Test",
			"contest_id":       contestID,
			"contest_class_id": chooseClass(contestID, classPratama),
		}, false, "", "", "").ExecuteTo(&bonsais); err != nil {
			log.Fatal("Failed to register bonsai: ", err)
		}
		bonsaiID := bonsais[0].ID
		log.Printf("[SUCCESS] Bonsai registered with ID %s\n", bonsaiID)

		log.Print("[TESTING] Finalizing bonsai...")
		_ = client.Rpc("finalize_bonsai", "", map[string]string{"bonsai_id": bonsaiID})
		log.Printf("[SUCCESS] Bonsai with ID %s has been finalized\n", bonsaiID)
		return bonsaiID
	}

	bonsaiID1 := registerAndFinalizeBonsai()
	bonsaiID2 := registerAndFinalizeBonsai()

	// --- Admin closes the registration
	log.Println("[INFO] Logging in as Admin...")
	login("admin")

	for _, bid := range []string{bonsaiID1, bonsaiID2} {
		log.Printf("[TESTING] Verifying bonsai with ID %s as an admin...\n", bid)
		_ = client.Rpc("verify_bonsai", "", map[string]string{"bonsai_id": bid})
		log.Printf("[SUCCESS] Bonsai with ID %s has been verified\n", bid)
	}

	log.Print("[TESTING] Closing contest as an admin...")
	if _, _, err := client.From("contests").Update(
		map[string]string{"state": "closed"}, "", "",
	).Eq("id", contestID).Execute(); err != nil {
		log.Fatal("Failed to close contest: ", err)
	}
	log.Println("[SUCCESS] Contest has been closed")

	// --- A contestant tries to register when the contest registration is closed
	log.Println("[INFO] Logging in as Demo...")
	login("demo")

	log.Print("[TESTING] Registering bonsai to a CLOSED contest as a contestant...")
	var bonsais []rowID
	if _, err := client.From("bonsai").Insert(map[string]string{
		"name":             "Test",
		"contest_id":       contestID,
		"contest_class_id": chooseClass(contestID, classPratama),
	}, false, "", "", "").ExecuteTo(&bonsais); err != nil {
		log.Println("[SUCCESS] Bonsai failed to register to a CLOSED contest")
	} else {
		log.Fatalf("[FAIL] Bonsai registered with ID %s to a CLOSED contest", bonsais[0].ID)
	}
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
