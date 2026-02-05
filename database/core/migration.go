// Package core - Mainly contains migration revision handler
package core

import (
	"database/sql"
	"errors"
	"fmt"
	"log"
	"os"
	"path/filepath"

	_ "github.com/jackc/pgx/v5/stdlib"  // Using pgx because for some reason pq keep returning EOF
)

type Revision struct {
	IsTest bool
	Timestamp int
	Description string
	FilePath string
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

	data, fetchErr := db.Query("SELECT * FROM public.schema_migrations")
	if fetchErr != nil {
		log.Fatal(fetchErr)
	}
	defer data.Close()

	for data.Next() {
		var (
			version		int64
			timestamp	string
		)
		if err := data.Scan(&version, &timestamp); err != nil {
			log.Fatal(err)
		}
		executed = append(executed, version)
	}
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

func (m *Migrations) Test() {
	entries, err := os.ReadDir(m.rootPath)
	if err != nil {
		log.Fatal(err)
	}

	for _, e := range entries {
		fmt.Println(e.Name())
	}
}
