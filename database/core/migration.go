package core

import (
	"database/sql"
	"fmt"
	"log"
	"os"
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

func NewMigrations(rootPath string, db *sql.DB) *Migrations {
	return &Migrations{
		rootPath: rootPath,
		db: db,
		revisions: make(map[int]Revision),
		executed: []int{},
	}
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
