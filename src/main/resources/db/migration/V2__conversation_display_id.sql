INSERT INTO conversation_display_id_counters (account_id, last_value)
SELECT id, 0 FROM accounts
ON CONFLICT (account_id) DO NOTHING;

CREATE OR REPLACE FUNCTION next_conversation_display_id(p_account_id INTEGER)
RETURNS INTEGER AS $$
DECLARE
  next_id INTEGER;
BEGIN
  INSERT INTO conversation_display_id_counters (account_id, last_value)
  VALUES (p_account_id, 1)
  ON CONFLICT (account_id)
  DO UPDATE SET last_value = conversation_display_id_counters.last_value + 1
  RETURNING last_value INTO next_id;
  RETURN next_id;
END;
$$ LANGUAGE plpgsql;
