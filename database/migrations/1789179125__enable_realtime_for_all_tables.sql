-- Creation Date: 2026-09-12 02:12:05.466176 +0000 UTC
-- Reason: enable realtime for all tables

-- Enable Supabase Realtime for all tables
ALTER PUBLICATION supabase_realtime ADD TABLE kodama.bonsai_classes;
ALTER PUBLICATION supabase_realtime ADD TABLE kodama.contests;
ALTER PUBLICATION supabase_realtime ADD TABLE kodama.contest_classes;
ALTER PUBLICATION supabase_realtime ADD TABLE kodama.contest_participants;
ALTER PUBLICATION supabase_realtime ADD TABLE kodama.bonsai;
ALTER PUBLICATION supabase_realtime ADD TABLE kodama.reviews;

