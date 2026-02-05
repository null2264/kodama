// Package core - Mainly contains migration revision handler
package core

import (
	"database/sql"
	"errors"
	"fmt"
	"log"
	"os"
	"path/filepath"
	_ "github.com/lib/pq"
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
	revisions map[int]Revision
	executed []int
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

	db, dbErr := sql.Open("postgres", dsn)

	cwd, cwdErr := os.Getwd()

	return &Migrations{
		rootPath: filepath.Join(cwd, "migrations"),
		db: db,
		revisions: make(map[int]Revision),
		executed: []int{},
	}, errors.Join(err, dbErr, cwdErr)
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
