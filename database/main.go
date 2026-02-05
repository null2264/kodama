package main

import (
	"fmt"
	supabase "github.com/supabase-community/supabase-go"
	"log"
	"os"
	"path/filepath"

	"github.com/joho/godotenv"
	"github.com/null2264/kodama/kodama-db/core"
	"github.com/spf13/cobra"
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
		migration, err := core.NewMigrations()
		if err != nil {
			log.Fatal(err)
		}
		migration.Reset()
	},
}

var (
	revisionReason string
	enableTestRev  bool
)

const (
	demoPass string = "demo12345"

	classMadya   string = "11943061-aa9d-4cc0-90f6-2ca7b70bc1b5"
	classProspek string = "8f8e46fd-75e9-443e-a1f2-7ffab68ece31"
	classPratama string = "fb85ea10-0fc5-40b9-9e27-f236962c8271"
)

func doTest(cmd *cobra.Command, args []string) {
	url := os.Getenv("SUPABASE_URL")
	key := os.Getenv("SUPABASE_KEY")
	client, err := supabase.NewClient(url, key, &supabase.ClientOptions{})
	if err != nil {
		log.Fatal("Failed to initalize the client: ", err)
	}

	session, err := client.SignInWithEmailPassword("demo@test.example.com", demoPass)
	if err != nil {
		log.Fatal("Sign in failed: ", err)
	}

	fmt.Println("User ID:", session.User.ID)
	fmt.Println("Access Token:", session.AccessToken)
}

func doMigrate(cmd *cobra.Command, args []string) {
	migration, err := core.NewMigrations()
	if err != nil {
		log.Fatal(err)
	}
	migration.Create(revisionReason, enableTestRev)
}

func init() {
	dbCmd.AddCommand(testCmd)
	dbCmd.AddCommand(migrateCmd)
	dbCmd.AddCommand(resetCmd)
	migrateCmd.Flags().StringVarP(&revisionReason, "reason", "r", "", "the reason for this revision")
	migrateCmd.MarkFlagRequired("reason")
	migrateCmd.Flags().BoolVarP(&enableTestRev, "test", "t", false, "mark this revision as a test revision")
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
		fmt.Println(err)
		os.Exit(1)
	}
}
