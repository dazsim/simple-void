Simple Void in-game smoke probe

Test-only NeoForge mod. Build with tools/smoke-probe.gradle and the distributed
Simple Books jar on SIMPLEVOID_BOOKS_JAR. It is copied only into the isolated
smoke server/client directories and never into the shipped Simple Void jar.

simplevoidsmoke status counts common and NeoForge area-state events.
simplevoidsmoke deny Dev dispatches a real area command with Dev's natural
player command source, not RCON's permissions. It checks the permission gate,
parser refusal and unchanged area state. execute as Dev would retain RCON's
permission and would not be a valid non-op test.

simplevoidsmoke claim Dev invokes the installed Simple Books QuestActionPacket
claim handler for the real online, non-operator player. That is the server path
used by the book Claim button. Quest completion, prior claims, reward permission
and area hooks are not bypassed. This is packet-handler integration coverage,
not an automated mouse click. The existing sbquests open server command opens
the real client quest screen for before/after screenshots. No GUI input events
are synthesized, and no Simple Books source or distribution is edited.

The smoke data pack is created by tools/create-smoke-pack.py. It changes only
this run's island area hooks and adds one Simple Books quest. Initial pack order
must include mod_data before file/simplevoid-smoke so the world pack overrides
bundled defaults before initial area snapshots are created.
