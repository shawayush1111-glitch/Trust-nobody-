package com.example.data

enum class TrapType(val displayName: String) {
    DISAPPEARING_FLOOR("Disappearing Floor"),
    MOVING_DOOR("Moving Door"),
    FAKE_CHECKPOINT("Fake Checkpoint"),
    LYING_BUTTON("Lying Button"),
    WRONG_ARROW("Wrong Arrow"),
    GRAVITY_FLIP("Gravity Flip"),
    FAKE_WALL("Fake Wall"),
    TILTING_PLATFORM("Tilting Platform"),
    FAKE_WIN("Fake Win Screen"),
    SPEED_FLOOR("Speed Floor"),
    SHRINKING_PLATFORM("Shrinking Platform"),
    COPY_PLATFORM("Copy Platform"),
    SPIKES("Instant Death Spikes")
}

data class Vector2D(var x: Float, var y: Float)

data class Platform(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val type: TrapType? = null,
    // Trap timing parameters
    var standsOn: Boolean = false,
    var progress: Float = 0f, // For disappearing/shrinking state
    var angle: Float = 0f,    // For tilting state
    var isVanished: Boolean = false
)

data class GameObject(
    val id: String,
    val type: TrapType,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    var extraData: String = "",
    var isActivated: Boolean = false,
    var message: String = ""
)

data class Level(
    val id: Int,
    val name: String,
    val description: String,
    val safePathDescription: String,
    val trapTypes: List<TrapType>,
    val platforms: List<Platform>,
    val objects: List<GameObject>,
    val startX: Float = 60f,
    val startY: Float = 400f,
    val exitX: Float = 880f,
    val exitY: Float = 440f,
    val isFakeWinLevel: Boolean = false,
    val bannerTip: String = ""
)

object LevelsDataSource {
    val levels: List<Level> = (1..50).map { id ->
        generateLevel(id)
    }

    private fun generateLevel(id: Int): Level {
        val platforms = mutableListOf<Platform>()
        val objects = mutableListOf<GameObject>()
        val trapTypes = mutableListOf<TrapType>()
        var startX = 60f
        var startY = 400f
        var exitX = 880f
        var exitY = 440f
        var levelName = ""
        var levelDesc = ""
        var safePath = ""
        var hasFakeWin = false
        var tip = ""

        // Base platform for starting position
        platforms.add(Platform("start", 20f, 480f, 180f, 50f))
        // Base platform for exit position
        platforms.add(Platform("exit_platform", 820f, 480f, 160f, 50f))

        when {
            // --- LEARNING PHASE (LEVELS 1 TO 10) ---
            id == 1 -> {
                levelName = "Easy Start?"
                levelDesc = "A completely straightforward warm up. Definitely no traps."
                safePath = "Leap cleanly across the center platform. Note: it disappears immediately when stepped on!"
                trapTypes.add(TrapType.DISAPPEARING_FLOOR)
                // Disappearing platform in the middle
                platforms.add(Platform("dis_floor_1", 380f, 480f, 240f, 40f, TrapType.DISAPPEARING_FLOOR))
                tip = "Nothing here can hurt you... except your own speed."
            }
            id == 2 -> {
                levelName = "Leap of Faith"
                levelDesc = "Test your vertical clearance over standard safety cones."
                safePath = "Double jump over the spike pit in the center directly onto the ledge."
                trapTypes.add(TrapType.SPIKES)
                // Spikes in center pit
                objects.add(GameObject("spikes_1", TrapType.SPIKES, 400f, 520f, 200f, 40f))
                platforms.add(Platform("mid_1", 450f, 380f, 100f, 30f))
                tip = "Don't fall in!"
            }
            id == 3 -> {
                levelName = "Checkpoint Falsehood"
                levelDesc = "We added a handy checkpoint in the absolute center. Trust it!"
                safePath = "The checkpoint in the center is fake and will reset you! Jump completely over it."
                trapTypes.add(TrapType.FAKE_CHECKPOINT)
                // Active fake checkpoint in center
                platforms.add(Platform("mid_1", 350f, 440f, 300f, 40f))
                objects.add(GameObject("fake_chk", TrapType.FAKE_CHECKPOINT, 480f, 390f, 40f, 50f, message = "CHECKPOINT SAVED! (Not Really)"))
                // Spikes after
                objects.add(GameObject("spikes_1", TrapType.SPIKES, 700f, 520f, 100f, 40f))
                tip = "Your progress is absolutely secure."
            }
            id == 4 -> {
                levelName = "The Arrow Knows"
                levelDesc = "Trust the signage. It has your absolute best interests at heart."
                safePath = "The signs point right into spikes. Drop straight down to the low platform or leap left."
                trapTypes.add(TrapType.WRONG_ARROW)
                platforms.add(Platform("mid_1", 350f, 420f, 200f, 40f))
                objects.add(GameObject("wrong_arrow_1", TrapType.WRONG_ARROW, 420f, 340f, 60f, 60f, extraData = "RIGHT"))
                // Spikes to the right of mid platform
                objects.add(GameObject("spikes_1", TrapType.SPIKES, 600f, 480f, 100f, 40f))
                platforms.add(Platform("secret_low", 580f, 300f, 100f, 30f))
                tip = "Signage is legally certified."
            }
            id == 5 -> {
                levelName = "Wrong Turn"
                levelDesc = "An arrow pointing right. Surely the exit is that way."
                safePath = "Going right kills you. Walk left, fall off, and land on the secret lower ledge."
                trapTypes.add(TrapType.WRONG_ARROW)
                // Trap arrows and platforms
                platforms.add(Platform("mid_high", 300f, 380f, 150f, 40f))
                platforms.add(Platform("mid_right", 550f, 380f, 150f, 40f))
                objects.add(GameObject("wrong_arrow", TrapType.WRONG_ARROW, 350f, 320f, 50f, 50f, extraData = "RIGHT"))
                objects.add(GameObject("spikes_r", TrapType.SPIKES, 550f, 340f, 150f, 40f)) // Spikes on the right platform!
                // Secret safe left path
                platforms.add(Platform("secret_left", 60f, 250f, 120f, 30f))
                tip = "When the arrow says left, read between the lines."
            }
            id == 6 -> {
                levelName = "The Safe Button"
                levelDesc = "Press the shiny safe green button to unlock the magical bridge."
                safePath = "Do not press the green safe button, it is a trap! Jump over it entirely."
                trapTypes.add(TrapType.LYING_BUTTON)
                platforms.add(Platform("mid_button_p", 380f, 480f, 240f, 40f))
                objects.add(GameObject("safe_btn", TrapType.LYING_BUTTON, 480f, 440f, 40f, 40f, message = "SAFE"))
                // Hidden spikes that trigger if button is pressed
                tip = "Button certified 100% hazard free."
            }
            id == 7 -> {
                levelName = "The Runaway Door"
                levelDesc = "The door is right there. Go get it!"
                safePath = "When you get close to the door, it slides right. Trigger it, jump back, then let it settle or chase it."
                trapTypes.add(TrapType.MOVING_DOOR)
                platforms.add(Platform("mid_walk", 300f, 480f, 400f, 40f))
                // The door will run away in game logic!
                exitX = 650f
                exitY = 440f
                tip = "The door seems a bit shy today."
            }
            id == 8 -> {
                levelName = "Tilted World"
                levelDesc = "Just a slippery slope. Maintain your footing."
                safePath = "Run across the tilting platform quickly before it slips down 45 degrees."
                trapTypes.add(TrapType.TILTING_PLATFORM)
                platforms.add(Platform("tilter_1", 350f, 480f, 300f, 40f, TrapType.TILTING_PLATFORM))
                tip = "A well-balanced mind survives slides."
            }
            id == 9 -> {
                levelName = "Ceiling Walker"
                levelDesc = "Up is the new down. Mind your head."
                safePath = "Let gravity flip you to the ceiling. Walk across the ceiling bar, then flip back."
                trapTypes.add(TrapType.GRAVITY_FLIP)
                // Platforms at normal level
                platforms.add(Platform("normal_mid", 300f, 480f, 150f, 40f))
                // Platform at top for ceiling walking
                platforms.add(Platform("ceiling_mid", 450f, 120f, 200f, 40f))
                objects.add(GameObject("grav_trigger", TrapType.GRAVITY_FLIP, 370f, 440f, 40f, 40f))
                tip = "Gravity is merely a suggestion."
            }
            id == 10 -> {
                levelName = "A Toast to Success"
                levelDesc = "You've proven your skill. Claim your effortless victory!"
                safePath = "This is a fake victory screen level! Reach the end, trigger the fake win, stay calm, and dodge the sudden trap."
                trapTypes.add(TrapType.FAKE_WIN)
                platforms.add(Platform("mid_finish", 300f, 480f, 450f, 40f))
                hasFakeWin = true
                tip = "Congratulations on finishing the tutorial phase!"
            }

            // --- DECEPTION PHASE (LEVELS 11 TO 25) ---
            id in 11..14 -> {
                levelName = "Deceptive Drift $id"
                levelDesc = "Multiple combinations of vanishing blocks and incorrect arrows."
                safePath = "Dodge the first floating block, step on the second briefly, and leap left instead of right."
                trapTypes.addAll(listOf(TrapType.DISAPPEARING_FLOOR, TrapType.WRONG_ARROW))
                platforms.add(Platform("df1", 250f, 440f, 180f, 35f, TrapType.DISAPPEARING_FLOOR))
                platforms.add(Platform("df2", 500f, 360f, 180f, 35f, TrapType.DISAPPEARING_FLOOR))
                objects.add(GameObject("spikes_btm", TrapType.SPIKES, 400f, 540f, 300f, 40f))
                tip = "Two wrongs make a right, but two arrows make a death."
            }
            id == 15 -> {
                levelName = "Arrow Paradox"
                levelDesc = "Every arrow points to safety. Every platform vanishes."
                safePath = "Run without stopping. Step on vanishing floors and ignore the indicators pointing backward."
                trapTypes.addAll(listOf(TrapType.DISAPPEARING_FLOOR, TrapType.WRONG_ARROW))
                platforms.add(Platform("df1", 250f, 460f, 120f, 35f, TrapType.DISAPPEARING_FLOOR))
                platforms.add(Platform("df2", 450f, 400f, 120f, 35f, TrapType.DISAPPEARING_FLOOR))
                platforms.add(Platform("df3", 650f, 340f, 120f, 35f, TrapType.DISAPPEARING_FLOOR))
                objects.add(GameObject("arrow_lies", TrapType.WRONG_ARROW, 480f, 320f, 50f, 50f, extraData = "LEFT"))
                tip = "Speed is your only friend."
            }
            id in 16..19 -> {
                levelName = "The Shrinking Maze $id"
                levelDesc = "Platforms grow narrower the longer you contemplate life."
                safePath = "Run continuously across the middle blocks. Standing idle will cause them to completely shrink."
                trapTypes.addAll(listOf(TrapType.SHRINKING_PLATFORM, TrapType.SPIKES))
                platforms.add(Platform("shrink_1", 280f, 440f, 150f, 40f, TrapType.SHRINKING_PLATFORM))
                platforms.add(Platform("shrink_2", 480f, 380f, 150f, 40f, TrapType.SHRINKING_PLATFORM))
                platforms.add(Platform("shrink_3", 680f, 440f, 150f, 40f, TrapType.SHRINKING_PLATFORM))
                objects.add(GameObject("spikes_p", TrapType.SPIKES, 300f, 540f, 400f, 45f))
                tip = "Losing weight is fast, losing platforms is faster."
            }
            id == 20 -> {
                levelName = "The Memory Leap"
                levelDesc = "The floor is gone. Only 3 invisible solid platforms remain."
                safePath = "Step exactly at X=250, X=450, and X=650. Any other step precipitates falling into spikes."
                trapTypes.addAll(listOf(TrapType.DISAPPEARING_FLOOR, TrapType.FAKE_WALL))
                // Fill pit with spikes, but we have a few custom solid platforms representing our safe spots
                platforms.add(Platform("safe_spot_1", 250f, 450f, 60f, 40f))
                platforms.add(Platform("safe_spot_2", 480f, 420f, 60f, 40f))
                platforms.add(Platform("safe_spot_3", 710f, 450f, 60f, 40f))
                // The rest is spiked
                objects.add(GameObject("spikes_mem", TrapType.SPIKES, 180f, 540f, 640f, 40f))
                tip = "Hope your memory is better than your reflexes."
            }
            id in 21..24 -> {
                levelName = "Velocity Trap $id"
                levelDesc = "These golden platforms carry a frictionless slide."
                safePath = "Jump immediately upon landing on the speed platforms to avoid being fired into the spike walls."
                trapTypes.addAll(listOf(TrapType.SPEED_FLOOR, TrapType.SPIKES))
                platforms.add(Platform("speed_1", 300f, 480f, 200f, 40f, TrapType.SPEED_FLOOR))
                platforms.add(Platform("speed_2", 550f, 420f, 200f, 40f, TrapType.SPEED_FLOOR))
                objects.add(GameObject("spikes_wall", TrapType.SPIKES, 750f, 360f, 40f, 100f))
                tip = "Caution: high acceleration ahead."
            }
            id == 25 -> {
                levelName = "Tilt and Flip"
                levelDesc = "Gravity reverses while platforms slide from under your feet."
                safePath = "Dodge the tilting boards. Let gravity carry you to the upper ceiling, walk right, then fall."
                trapTypes.addAll(listOf(TrapType.TILTING_PLATFORM, TrapType.GRAVITY_FLIP))
                platforms.add(Platform("tilt_combine", 280f, 480f, 180f, 40f, TrapType.TILTING_PLATFORM))
                platforms.add(Platform("ceiling_combine", 550f, 140f, 200f, 40f))
                objects.add(GameObject("gravity_flip_trig", TrapType.GRAVITY_FLIP, 320f, 440f, 40f, 40f))
                tip = "Up is down, down is tilted."
            }

            // --- CHAOS PHASE (LEVELS 26 TO 40) ---
            id == 30 -> {
                levelName = "Spike Paradise"
                levelDesc = "Every surface is beautifully crimson. Everything is a trap."
                safePath = "Step only on the single tiny platform wrapped in fake-wall overlay. Walk directly through the vertical barrier."
                trapTypes.addAll(listOf(TrapType.FAKE_WALL, TrapType.SPIKES))
                platforms.add(Platform("top_bar", 300f, 300f, 400f, 40f))
                // Spikes everywhere
                objects.add(GameObject("spikes_everywhere_1", TrapType.SPIKES, 200f, 520f, 600f, 40f))
                objects.add(GameObject("spikes_everywhere_2", TrapType.SPIKES, 300f, 260f, 400f, 40f))
                platforms.add(Platform("mid_hidden", 500f, 420f, 80f, 40f, TrapType.FAKE_WALL))
                tip = "You can walk through things that look scary."
            }
            id == 35 -> {
                levelName = "Double Jeopardy"
                levelDesc = "Two identical pathways. Choose wisely... or don't."
                safePath = "Take the lower pathway. The upper loop ends in fake-wall drop down directly into vertical spikes."
                trapTypes.addAll(listOf(TrapType.COPY_PLATFORM, TrapType.SPIKES))
                // Platform duplication
                platforms.add(Platform("upper_way", 300f, 350f, 400f, 30f))
                platforms.add(Platform("lower_way", 300f, 480f, 400f, 30f))
                // Lying decoration
                objects.add(GameObject("spikes_lower", TrapType.SPIKES, 480f, 440f, 50f, 40f)) // Lower way spikes are fake walls!
                objects.add(GameObject("spikes_upper", TrapType.SPIKES, 480f, 310f, 50f, 30f)) // Real spikes on upper path!
                tip = "Don't believe what you see."
            }
            id == 40 -> {
                levelName = "The Checkpoint Trap"
                levelDesc = "A safe checkpoint that sends you to an even tighter platform."
                safePath = "Skip the fake red checkpoint. Leaping past it lets you run on the standard exit route."
                trapTypes.addAll(listOf(TrapType.FAKE_CHECKPOINT, TrapType.LYING_BUTTON, TrapType.SPIKES))
                platforms.add(Platform("main_path", 250f, 440f, 500f, 40f))
                objects.add(GameObject("fake_chk_40", TrapType.FAKE_CHECKPOINT, 400f, 390f, 40f, 50f, message = "SAVE GAME"))
                // Lying safe button
                objects.add(GameObject("safe_btn_40", TrapType.LYING_BUTTON, 550f, 400f, 35f, 40f, message = "SAFE"))
                tip = "Saves can be extremely dangerous."
            }
            id in 26..39 -> {
                levelName = "Chaos Chamber $id"
                levelDesc = "Multiple items triggering concurrently: tilted boards, shrinking zones, buttons."
                safePath = "Avoid the green safe button, slide down the tilt board, and land on the moving block."
                trapTypes.addAll(listOf(TrapType.TILTING_PLATFORM, TrapType.SHRINKING_PLATFORM, TrapType.LYING_BUTTON))
                platforms.add(Platform("tilt_ch", 250f, 480f, 160f, 40f, TrapType.TILTING_PLATFORM))
                platforms.add(Platform("shrink_ch", 450f, 400f, 160f, 40f, TrapType.SHRINKING_PLATFORM))
                objects.add(GameObject("btn_ch", TrapType.LYING_BUTTON, 500f, 360f, 35f, 40f, message = "SAFE"))
                tip = "Good luck keeping your feet underneath you."
            }

            // --- PURE EVIL PHASE (LEVELS 41 TO 50) ---
            id == 45 -> {
                levelName = "The Gravity Flop"
                levelDesc = "Gravity polarities change constantly with zero warning."
                safePath = "Count 3 seconds. Run when gravity flips up onto the ceiling, then proceed to the exit."
                trapTypes.addAll(listOf(TrapType.GRAVITY_FLIP, TrapType.DISAPPEARING_FLOOR))
                platforms.add(Platform("dis_bot1", 250f, 480f, 150f, 40f, TrapType.DISAPPEARING_FLOOR))
                platforms.add(Platform("dis_top1", 450f, 120f, 150f, 40f, TrapType.DISAPPEARING_FLOOR))
                platforms.add(Platform("dis_bot2", 650f, 480f, 150f, 40f, TrapType.DISAPPEARING_FLOOR))
                tip = "Hope you aren't prone to motion sickness."
            }
            id == 48 -> {
                levelName = "Infinite Deception"
                levelDesc = "Three fake win indicators designed to maximize total screen rage."
                safePath = "Dodge the first exit door, walk straight through the second fake exit, and look for the hidden actual door on the bottom platform!"
                trapTypes.addAll(listOf(TrapType.FAKE_WIN, TrapType.MOVING_DOOR, TrapType.FAKE_WALL))
                platforms.add(Platform("path_1_48", 200f, 440f, 200f, 40f))
                platforms.add(Platform("path_2_48", 450f, 440f, 200f, 40f))
                platforms.add(Platform("path_3_48", 700f, 440f, 200f, 40f))
                // Multiple doors
                objects.add(GameObject("fake_door_1", TrapType.MOVING_DOOR, 350f, 390f, 40f, 50f))
                objects.add(GameObject("fake_door_2", TrapType.MOVING_DOOR, 550f, 390f, 40f, 50f))
                hasFakeWin = true
                tip = "Nothing here is real."
            }
            id == 50 -> {
                levelName = "FINAL BOSS: Trust Nobody"
                levelDesc = "The ultimate trials. All 12 traps active concurrently. Win this and get mocked forever."
                safePath = "Leap off the tilted platform, skip the green button, cross the shrinking blocks, slide down the fake wall, and sprint to the portal."
                trapTypes.addAll(TrapType.values().toList())
                platforms.add(Platform("p50_tilt", 220f, 460f, 140f, 40f, TrapType.TILTING_PLATFORM))
                platforms.add(Platform("p50_shrink", 400f, 380f, 140f, 40f, TrapType.SHRINKING_PLATFORM))
                platforms.add(Platform("p50_dis", 580f, 440f, 140f, 40f, TrapType.DISAPPEARING_FLOOR))
                objects.add(GameObject("lying_btn_50", TrapType.LYING_BUTTON, 450f, 340f, 40f, 40f, message = "DO NOT PRESS"))
                platforms.add(Platform("fake_w50", 750f, 400f, 100f, 40f, TrapType.FAKE_WALL))
                objects.add(GameObject("fake_chk_50", TrapType.FAKE_CHECKPOINT, 300f, 410f, 40f, 50f, message = "CHECKPOINT"))
                exitX = 900f
                exitY = 440f
                tip = "Your final destination is ready, mortal."
            }
            else -> {
                // Procedurally generated high difficulty chaos layouts for levels 41-49
                levelName = "Dark Abyss of Agony $id"
                levelDesc = "An elegant nightmare generated for your personal rage."
                safePath = "Leap onto the higher platform, dodge the floor traps, jump mid-air when gravity flips, and land on the exit deck."
                trapTypes.addAll(listOf(TrapType.DISAPPEARING_FLOOR, TrapType.TILTING_PLATFORM, TrapType.SPIKES))
                platforms.add(Platform("proc_tilt", 250f, 480f, 180f, 35f, TrapType.TILTING_PLATFORM))
                platforms.add(Platform("proc_dis", 480f, 400f, 180f, 35f, TrapType.DISAPPEARING_FLOOR))
                platforms.add(Platform("proc_solid", 710f, 480f, 100f, 35f))
                objects.add(GameObject("proc_spikes", TrapType.SPIKES, 300f, 540f, 450f, 40f))
                tip = "Every choice is a leap into the void."
            }
        }

        return Level(
            id = id,
            name = levelName,
            description = levelDesc,
            safePathDescription = safePath,
            trapTypes = trapTypes,
            platforms = platforms,
            objects = objects,
            startX = startX,
            startY = startY,
            exitX = exitX,
            exitY = exitY,
            isFakeWinLevel = hasFakeWin,
            bannerTip = tip
        )
    }
}
