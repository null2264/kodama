package main

import (
	"fmt"
	supabase "github.com/supabase-community/supabase-go"
	"log"
	"os"

	"github.com/joho/godotenv"
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

const demoPass string = "demo12345"

const classMadya string = "11943061-aa9d-4cc0-90f6-2ca7b70bc1b5"
const classProspek string = "8f8e46fd-75e9-443e-a1f2-7ffab68ece31"
const classPratama string = "fb85ea10-0fc5-40b9-9e27-f236962c8271"

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

	fmt.Printf("User ID: %s\n", session.User.ID)
	fmt.Printf("Access Token: %s\n", session.AccessToken)
}

func init() {
	dbCmd.AddCommand(testCmd)
	rootCmd.AddCommand(dbCmd)
}

func main() {
	godotenv.Load(".env.local")
	godotenv.Load()

	if err := rootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}
