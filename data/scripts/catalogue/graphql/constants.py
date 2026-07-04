"""
Constant values.
"""

query = """mutation change($tables: [MolgenisTableInput]) {
  change(tables: $tables) {
    message
  }
}"""
