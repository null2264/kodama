// Package core - Mainly contains migration revision handler
package core

import (
	"database/sql"
	"errors"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"regexp"
	"slices"
	"time"

	_ "github.com/jackc/pgx/v5/stdlib" // Using pgx because for some reason pq keep returning EOF
)

type Revision struct {
	isTest bool
	timestamp int64
	description string
	filePath string
}

type Migrations struct {
	rootPath string
	db *sql.DB
	revisions map[int64]Revision
	executed []int64
}

type pgError struct {
	message string
}

func (e *pgError) Error() string {
    return e.message
}

func NewMigrations() (*Migrations, error) {
	var err error = nil
	dsn, ok := os.LookupEnv("PG_URI")
	if !ok {
		err = &pgError{message: "PG_URI is not set"}
	}

	db, dbErr := sql.Open("pgx", dsn)
	if dbErr != nil {
		err = errors.Join(err, dbErr)
	}

	executed := []int64{}

	//#region schema migrations storage
	_, _ = db.Exec(`
		CREATE TABLE IF NOT EXISTS public.schema_migrations (
			version BIGINT PRIMARY KEY,
			applied_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
		)
	`)
	_, _ = db.Exec("ALTER TABLE public.schema_migrations ENABLE ROW LEVEL SECURITY")

	data, fetchErr := db.Query("SELECT version FROM public.schema_migrations ORDER BY version ASC")
	if fetchErr != nil {
		log.Fatal(fetchErr)
	}
	defer data.Close()

	for data.Next() {
		var (
			version		int64
		)
		if err := data.Scan(&version); err != nil {
			log.Fatal(err)
		}
		executed = append(executed, version)
	}
	//slices.Contains(executed, value)
	//#endregion

	cwd, cwdErr := os.Getwd()
	if cwdErr != nil {
		err = errors.Join(err, cwdErr)
	}

	return &Migrations{
		rootPath: filepath.Join(cwd, "migrations"),
		db: db,
		revisions: make(map[int64]Revision),
		executed: executed,
	}, err
}

func (m *Migrations) Reset() {
	if _, err := m.db.Exec("DROP SCHEMA IF EXISTS kodama CASCADE"); err != nil {
		log.Fatal(err)
	}

	if _, err := m.db.Exec("DELETE FROM auth.users WHERE email ~ '@test.example.com$'"); err != nil {
		log.Fatal(err)
	}

	if _, err := m.db.Exec("DELETE FROM public.schema_migrations"); err != nil {
		log.Fatal(err)
	}
}

func If[T any](cond bool, a, b T) T {
    if cond { return a }
    return b
}

func (m *Migrations) isNextRevisionTaken(revision int64) bool {
	return slices.Contains(m.executed, revision)
}

func (m *Migrations) Create(reason string, isTest bool) Revision {
	now := time.Now()
	timestamp := now.UnixMilli()
	if m.isNextRevisionTaken(timestamp) {
		log.Fatal("Revision is already exists")
	}

	re := regexp.MustCompile(`\s`)
	cleaned := re.ReplaceAllString(reason, "_")
	filename := fmt.Sprint(If(isTest, "T", ""), timestamp, "__", cleaned, ".sql")
	path := filepath.Join(m.rootPath, filename)
	stub := fmt.Sprintln("-- Creation Date:", now.UTC(), "\n-- Reason:", reason, "\n")

	rev := Revision{
		isTest: isTest,
		description: reason,
		timestamp: timestamp,
		filePath: path,
	}

	err := os.WriteFile(path, []byte(stub), 0644)
	if err != nil {
		log.Fatal(err)
	} else {
		fmt.Print("Successfully created ", If(isTest, "test ", ""), "revision '", filename, "'\n")
	}

	return rev
}

func (m *Migrations) Test() {
	entries, err := os.ReadDir(m.rootPath)
	if err != nil {
		log.Fatal(err)
	}

	for _, e := range entries {
		fmt.Println(e.Name())
	}
}
