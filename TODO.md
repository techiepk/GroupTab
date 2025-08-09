# TODO

- [ ] Credit card transactions handling
- [X] Check user video on model download problem
- [X] Check calculations
- [X] show date
- [ ] data export
- [X] all chip view is broken

- [ ] add chart for existing bank supports and templates

## Logo
- [X] Meesho
- [X] jio smartpoint

## Delete Transactions Feature
- [ ] Add deleted_transaction_hashes table to database
- [ ] Create migration for new table
- [ ] Add DAO methods for deleted hashes (insert, check, cleanup)
- [ ] Update TransactionRepository delete method to track hash
- [ ] Update TransactionRepository insert to skip deleted hashes
- [ ] Create SwipeableTransactionItem with red delete background
- [ ] Add delete function to TransactionsViewModel
- [ ] Add undo functionality with snackbar (5 second window)
- [ ] Update TransactionsScreen with swipe-to-delete UI

## Smart SMS Scanning
- [ ] Add last_scan_timestamp to UserPreferences
- [ ] Update SmsReaderWorker - first scan: 3 months, then: last 2 days
- [ ] Save timestamp after each successful scan
- [ ] Test with week-old unscanned messages

## Testing
- [ ] Verify deleted transactions don't reappear on rescan
- [ ] Test undo works within timeout
- [ ] Check performance with 1000+ transactions

## Future Work
- [ ] Fix transaction hash collision issue (add more unique data)
- [ ] Bulk delete feature
- [ ] Full rescan option in settings
- [ ] Cleanup deleted hashes older than 6 months


## AI

- [ ] BYOK
