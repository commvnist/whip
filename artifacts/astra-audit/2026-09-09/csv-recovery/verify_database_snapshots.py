"""Compare synthetic databases produced by process_csv_recovery.py."""
import json
import pathlib
import sqlite3
import sys

directory = pathlib.Path(sys.argv[1])
snapshots = {}
for name in ("unchanged", "changed", "source-removed", "final"):
    path = directory / (name + ".db")
    assert path.is_file(), path
    with sqlite3.connect(path) as database:
        assert database.execute("PRAGMA integrity_check").fetchone() == ("ok",)
        tables = {}
        for table in ("track_entries", "track_values", "track_csv_import_receipts"):
            cursor = database.execute("SELECT * FROM " + table + " ORDER BY rowid")
            tables[table] = dict(columns=[column[0] for column in cursor.description], rows=cursor.fetchall())
        snapshots[name] = tables
assert all(not table["rows"] for name in ("unchanged", "changed") for table in snapshots[name].values())
assert snapshots["source-removed"] == snapshots["final"]
assert [len(table["rows"]) for table in snapshots["final"].values()] == [2, 14, 1]
(directory / "complete-database-proof.json").write_text(json.dumps(
    dict(allColumnsEqualAfterRecovery=True, snapshots=snapshots), indent=2) + "\n")
print("Verified all columns: two Entries, fourteen values, one receipt.")
