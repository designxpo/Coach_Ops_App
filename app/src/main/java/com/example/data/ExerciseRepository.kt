package com.example.data

object ExerciseRepository {

    val all: List<Exercise> = buildList {

        // ── STRENGTH ──────────────────────────────────────────────────────────

        add(Exercise(
            id = "push_up",
            name = "Push-Up",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.CORE),
            equipment = EquipmentType.BODYWEIGHT,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.BUILD_MUSCLE, ClientGoal.GENERAL_FITNESS, ClientGoal.LOSE_FAT),
            sets = "3–4 sets", reps = "10–20 reps", tempo = "2-1-1-0", rest = "45–60 sec",
            howTo = listOf(
                "Place hands shoulder-width apart, fingers pointing forward.",
                "Keep body in a straight line from head to heels — brace your core.",
                "Lower chest to 2–3 cm from the floor by bending elbows at 45°.",
                "Press back to the start. Exhale on the way up."
            ),
            commonErrors = listOf(
                "Sagging hips — core is not braced.",
                "Flaring elbows out to 90° — strains shoulder joint.",
                "Partial range of motion — reduces chest activation."
            ),
            benefits = listOf("Builds chest, shoulders, and triceps simultaneously", "Strengthens the core isometrically", "Improves upper-body pushing strength with zero equipment"),
            bodyEffect = "Activates pectoralis major (stretch-shortening cycle), long head of triceps, and anterior deltoid. The eccentric phase causes micro-tears repaired during rest → hypertrophy.",
            caloriesBurned = "~7–10 kcal/min",
            muscleEmoji = "💪",
            estimatedMinutes = 20,
            imageUrl = "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "squat",
            name = "Bodyweight Squat",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CORE, MuscleGroup.CALVES),
            equipment = EquipmentType.BODYWEIGHT,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.BUILD_MUSCLE, ClientGoal.LOSE_FAT, ClientGoal.GENERAL_FITNESS),
            sets = "3–4 sets", reps = "12–20 reps", tempo = "3-1-2-0", rest = "60 sec",
            howTo = listOf(
                "Stand feet shoulder-width, toes 15–30° out.",
                "Brace core, keep chest tall. Begin by pushing hips back.",
                "Lower until thighs are parallel to the floor (or deeper if mobility allows).",
                "Drive through heels to stand. Squeeze glutes at top."
            ),
            commonErrors = listOf(
                "Knees caving inward (valgus collapse).",
                "Heels lifting — limited ankle mobility.",
                "Rounding the lower back at the bottom."
            ),
            benefits = listOf("Builds quad and glute mass", "Improves hip mobility and knee stability", "Largest lower-body compound movement — high calorie burn"),
            bodyEffect = "Stimulates quads (rectus femoris, vastus group), gluteus maximus, and adductors. Depth determines glute vs quad emphasis. Causes significant metabolic demand due to large muscle mass involved.",
            caloriesBurned = "~8–12 kcal/min",
            muscleEmoji = "🦵",
            estimatedMinutes = 25,
            imageUrl = "https://images.unsplash.com/photo-1544067032-81da16caebd2?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "deadlift_bb",
            name = "Barbell Deadlift",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
            secondaryMuscles = listOf(MuscleGroup.CORE, MuscleGroup.FOREARMS, MuscleGroup.QUADS),
            equipment = EquipmentType.BARBELL,
            difficulty = DifficultyLevel.INTERMEDIATE,
            suitableFor = listOf(ClientGoal.BUILD_MUSCLE, ClientGoal.GENERAL_FITNESS),
            sets = "3–5 sets", reps = "3–8 reps", tempo = "2-0-1-1", rest = "2–3 min",
            howTo = listOf(
                "Bar over mid-foot, feet hip-width. Grip just outside legs.",
                "Hinge at hips, neutral spine. Bar close to shins.",
                "Brace hard — 360° core tension. Drive floor away.",
                "Lock out by squeezing glutes. Lower with control, hinge first."
            ),
            commonErrors = listOf("Rounding the lower back (most dangerous)", "Bar drifting forward — increases lever arm", "Jerking the bar — momentum replaces strength"),
            benefits = listOf("Develops posterior chain (back, glutes, hamstrings) comprehensively", "Increases grip strength", "Greatest hormonal stimulus of any exercise (testosterone, GH)"),
            bodyEffect = "Recruits erector spinae, glutes, hamstrings, and lats. The isometric back hold develops spinal erectors. High CNS demand → leads to strength adaptation via neural recruitment.",
            caloriesBurned = "~10–14 kcal/min",
            muscleEmoji = "🔙",
            estimatedMinutes = 40,
            imageUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "bench_press_db",
            name = "Dumbbell Bench Press",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.CHEST),
            secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            equipment = EquipmentType.DUMBBELL,
            difficulty = DifficultyLevel.INTERMEDIATE,
            suitableFor = listOf(ClientGoal.BUILD_MUSCLE),
            sets = "3–4 sets", reps = "8–12 reps", tempo = "3-1-2-0", rest = "90 sec",
            howTo = listOf(
                "Lie on bench. Dumbbells at chest level, palms forward.",
                "Arch back slightly, feet flat. Retract scapula.",
                "Press up and slightly in — dumbbells touch at the top.",
                "Lower slowly until elbows are 45° below shoulder height."
            ),
            commonErrors = listOf("Bouncing off chest", "Wrists bending back under load", "Not retracting scapula — reduces chest tension"),
            benefits = listOf("Greater range of motion than barbell", "Corrects strength imbalances between sides", "Safer for shoulder joints"),
            bodyEffect = "Sternal head of pec major under maximum stretch at bottom — crucial for hypertrophy. DBs allow more natural arc of motion reducing impingement risk.",
            caloriesBurned = "~6–8 kcal/min",
            muscleEmoji = "💪",
            estimatedMinutes = 30,
            imageUrl = "https://images.unsplash.com/photo-1540497077202-7c8a3999166f?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "pull_up",
            name = "Pull-Up",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.CORE),
            equipment = EquipmentType.PULL_UP_BAR,
            difficulty = DifficultyLevel.INTERMEDIATE,
            suitableFor = listOf(ClientGoal.BUILD_MUSCLE, ClientGoal.GENERAL_FITNESS),
            sets = "3–4 sets", reps = "5–12 reps", tempo = "2-1-3-0", rest = "90 sec",
            howTo = listOf(
                "Hang with overhand grip slightly wider than shoulders.",
                "Depress and retract scapula before pulling.",
                "Drive elbows toward hips, chin over bar.",
                "Lower slowly — 3 seconds. Full extension at bottom."
            ),
            commonErrors = listOf("Kipping / using momentum", "Not reaching full extension — reduces lat stretch", "Shrugging shoulders up"),
            benefits = listOf("Best bodyweight back exercise", "Develops lat width for V-taper", "Strengthens biceps and grip simultaneously"),
            bodyEffect = "Latissimus dorsi undergoes full stretch-to-peak-contraction ROM. The slow eccentric heavily targets long head of biceps. Improved shoulder stability.",
            caloriesBurned = "~8–10 kcal/min",
            muscleEmoji = "🔙",
            estimatedMinutes = 25,
            imageUrl = "https://images.unsplash.com/photo-1574680096145-d05b474e2155?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "shoulder_press_db",
            name = "Dumbbell Shoulder Press",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.SHOULDERS),
            secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.CORE),
            equipment = EquipmentType.DUMBBELL,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.BUILD_MUSCLE, ClientGoal.GENERAL_FITNESS),
            sets = "3–4 sets", reps = "10–15 reps", tempo = "2-1-2-0", rest = "60–90 sec",
            howTo = listOf(
                "Sit upright, back supported. Dumbbells at ear level, elbows 90°.",
                "Press directly overhead — don't flare forward.",
                "Bring dumbbells close at top without locking elbows hard.",
                "Lower slowly back to start."
            ),
            commonErrors = listOf("Leaning back (lumbar hyperextension)", "Pressing in front of head rather than straight up", "Flaring elbows forward at start"),
            benefits = listOf("Develops all three deltoid heads", "Increases overhead stability", "Improves posture"),
            bodyEffect = "Anterior and medial deltoids are primary movers. Supraspinatus stabilises shoulder joint. Triceps provide elbow extension power.",
            caloriesBurned = "~5–7 kcal/min",
            muscleEmoji = "🏋️",
            estimatedMinutes = 30,
            imageUrl = "https://images.unsplash.com/photo-1581009137042-c552e485697a?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "plank",
            name = "Plank",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.CORE),
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.GLUTES),
            equipment = EquipmentType.BODYWEIGHT,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.BUILD_MUSCLE, ClientGoal.LOSE_FAT, ClientGoal.GENERAL_FITNESS),
            sets = "3 sets", reps = "30–90 sec hold", tempo = "Hold", rest = "45–60 sec",
            howTo = listOf(
                "Forearms on floor, elbows under shoulders.",
                "Body perfectly straight — squeeze glutes and quads.",
                "Pull navel slightly toward spine — brace transverse abdominis.",
                "Breathe normally. Do not hold breath."
            ),
            commonErrors = listOf("Hips sagging down", "Hips piking too high", "Head dropping / looking forward"),
            benefits = listOf("Strengthens the anti-extension core", "Transfers to every sport and lift", "Reduces lower-back pain risk"),
            bodyEffect = "Transverse abdominis (deep core) acts isometrically to resist spinal extension. Rectus abdominis and obliques co-contract. This builds core stiffness — the foundation of all compound lifts.",
            caloriesBurned = "~3–5 kcal/min",
            muscleEmoji = "🎯",
            estimatedMinutes = 15,
            imageUrl = "https://images.unsplash.com/photo-1566241142559-40e1dab266c6?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "lunge",
            name = "Walking Lunge",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES, MuscleGroup.CORE),
            equipment = EquipmentType.BODYWEIGHT,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.BUILD_MUSCLE, ClientGoal.LOSE_FAT, ClientGoal.GENERAL_FITNESS),
            sets = "3 sets", reps = "12 reps per leg", tempo = "2-1-2-0", rest = "60 sec",
            howTo = listOf(
                "Stand upright. Step forward with one foot.",
                "Lower rear knee toward floor — stop 2 cm before touching.",
                "Front knee tracks over second toe. Push off heel to next step.",
                "Alternate legs for prescribed reps."
            ),
            commonErrors = listOf("Front knee caving inward", "Torso leaning too far forward", "Short stride — puts excessive knee stress"),
            benefits = listOf("Unilateral — corrects leg strength imbalances", "Improves hip flexor mobility", "High glute activation"),
            bodyEffect = "Step length determines glute vs quad bias. Longer stride = more glute activation. Shorter stride = more quad dominant. Also develops single-leg stability through hip abductors.",
            caloriesBurned = "~6–9 kcal/min",
            muscleEmoji = "🦵",
            estimatedMinutes = 20,
            imageUrl = "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "bicep_curl_db",
            name = "Dumbbell Bicep Curl",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.BICEPS),
            secondaryMuscles = listOf(MuscleGroup.FOREARMS),
            equipment = EquipmentType.DUMBBELL,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.BUILD_MUSCLE),
            sets = "3–4 sets", reps = "10–15 reps", tempo = "2-1-3-0", rest = "60 sec",
            howTo = listOf(
                "Stand, arms hanging, palms forward.",
                "Curl dumbbell toward shoulder — keep elbow pinned to side.",
                "Squeeze bicep hard at peak.",
                "Lower slowly in 3 seconds — feel the stretch."
            ),
            commonErrors = listOf("Swinging the torso (cheating)", "Elbows drifting forward", "Short range of motion — not extending fully"),
            benefits = listOf("Direct bicep hypertrophy", "Improves grip strength and endurance", "Better arm aesthetics"),
            bodyEffect = "Biceps brachii (long and short head) contract concentrically. Slow eccentric produces the most muscle damage per rep. Brachialis and brachioradialis synergise.",
            caloriesBurned = "~4–6 kcal/min",
            muscleEmoji = "💪",
            estimatedMinutes = 25,
            imageUrl = "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=400&h=500&fit=crop&q=80"
        ))

        // ── YOGA ─────────────────────────────────────────────────────────────

        add(Exercise(
            id = "surya_namaskar",
            name = "Surya Namaskar",
            sanskritName = "Sun Salutation",
            category = ExerciseCategory.YOGA,
            primaryMuscles = listOf(MuscleGroup.FULL_BODY),
            secondaryMuscles = emptyList(),
            equipment = EquipmentType.MAT,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.GENERAL_FITNESS, ClientGoal.IMPROVE_FLEXIBILITY, ClientGoal.LOSE_FAT),
            sets = "5–12 rounds", reps = "1 round = 12 poses", tempo = "Breath-linked", rest = "None between rounds",
            howTo = listOf(
                "Pranamasana: Stand in Tadasana, palms together at chest.",
                "Hastauttanasana: Inhale, raise arms overhead, slight backbend.",
                "Uttanasana: Exhale, fold forward — hands to floor.",
                "Ashwa Sanchalanasana: Inhale, right leg back, knee down, look up.",
                "Parvatasana: Exhale, both legs back — Downward Dog.",
                "Ashtanga Namaskar: Lower to 8-point (knees, chest, chin, hands, feet).",
                "Bhujangasana: Inhale, press chest up — Cobra.",
                "Parvatasana again: Exhale, Downward Dog.",
                "Ashwa Sanchalanasana: Inhale, step right foot forward.",
                "Uttanasana: Exhale, fold forward.",
                "Hastauttanasana: Inhale, rise up — overhead stretch.",
                "Pranamasana: Exhale, palms together. Repeat other side."
            ),
            commonErrors = listOf("Holding breath between poses", "Sinking lower back in cobra", "Hips too high in Ashwa Sanchalanasana"),
            benefits = listOf("Warms up every joint in 12 minutes", "Improves cardiovascular endurance", "Reduces morning stiffness and cortisol"),
            bodyEffect = "Rhythmic breath-movement synchronisation activates parasympathetic nervous system. Spinal flexion-extension lubricates discs. 1 round at moderate pace burns ~4–5 kcal.",
            caloriesBurned = "~4–8 kcal/min",
            muscleEmoji = "🧘",
            estimatedMinutes = 30,
            imageUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "warrior_2",
            name = "Warrior II",
            sanskritName = "Virabhadrasana II",
            category = ExerciseCategory.YOGA,
            primaryMuscles = listOf(MuscleGroup.QUADS, MuscleGroup.HIP_FLEXORS),
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.SHOULDERS),
            equipment = EquipmentType.MAT,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.IMPROVE_FLEXIBILITY, ClientGoal.GENERAL_FITNESS),
            sets = "Hold each side", reps = "30–60 sec per side", tempo = "Static hold", rest = "Transition to next pose",
            howTo = listOf(
                "Step feet 4 feet apart. Turn right foot out 90°, left foot in 15°.",
                "Bend right knee over second toe — thigh parallel to floor.",
                "Extend arms parallel to floor. Gaze over front fingertips.",
                "Square hips to side. Breathe into the posture."
            ),
            commonErrors = listOf("Front knee collapsing inward", "Torso leaning forward", "Raised shoulders — create tension not strength"),
            benefits = listOf("Opens hips and groins", "Strengthens legs isometrically", "Improves hip flexor mobility"),
            bodyEffect = "Sustained isometric contraction of quadriceps and hip abductors. Lengthens hip flexor of the back leg. Improves hip ROM — directly translates to better squat depth.",
            caloriesBurned = "~3–5 kcal/min",
            muscleEmoji = "🧘",
            estimatedMinutes = 20,
            imageUrl = "https://images.unsplash.com/photo-1575052814086-f385e2e2ad1b?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "downward_dog",
            name = "Downward-Facing Dog",
            sanskritName = "Adho Mukha Svanasana",
            category = ExerciseCategory.YOGA,
            primaryMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES, MuscleGroup.SPINE),
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.CORE),
            equipment = EquipmentType.MAT,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.IMPROVE_FLEXIBILITY, ClientGoal.GENERAL_FITNESS),
            sets = "3–5 holds", reps = "30–60 sec hold", tempo = "Static", rest = "Child's pose between",
            howTo = listOf(
                "Start on hands and knees — wrists under shoulders.",
                "Tuck toes, lift knees. Press hips up and back.",
                "Straighten legs as much as flexibility allows. Heels toward floor.",
                "Press hands firmly — spread fingers. Lengthen spine."
            ),
            commonErrors = listOf("Rounding upper back", "Locking knees hard — bends spine", "Weight dumped into wrists — distribute through fingers"),
            benefits = listOf("Lengthens hamstrings and calves", "Decompresses the spine", "Strengthens shoulders and arms"),
            bodyEffect = "Gravity creates traction along the spine decompressing lumbar discs. Hamstring group is placed in sustained eccentric stretch — improves passive flexibility over time.",
            caloriesBurned = "~3–4 kcal/min",
            muscleEmoji = "🧘",
            estimatedMinutes = 15,
            imageUrl = "https://images.unsplash.com/photo-1599901860904-17e6ed7083a0?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "cobra",
            name = "Cobra Pose",
            sanskritName = "Bhujangasana",
            category = ExerciseCategory.YOGA,
            primaryMuscles = listOf(MuscleGroup.SPINE, MuscleGroup.CHEST),
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.GLUTES),
            equipment = EquipmentType.MAT,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.IMPROVE_FLEXIBILITY, ClientGoal.GENERAL_FITNESS),
            sets = "3–5 holds", reps = "20–30 sec hold", tempo = "Breath-linked", rest = "Balasana between",
            howTo = listOf(
                "Lie face down. Palms under shoulders, elbows tucked.",
                "Inhale and press chest up. Keep lower ribs on mat (low cobra).",
                "Roll shoulders back and down. Gaze forward or slightly up.",
                "Exhale and lower slowly."
            ),
            commonErrors = listOf("Pushing up too high — compresses lumbar", "Straight arms (up-dog vs cobra distinction)", "Shoulders raised to ears"),
            benefits = listOf("Counteracts desk-posture (thoracic kyphosis)", "Strengthens spinal extensors", "Opens chest and lungs"),
            bodyEffect = "Erector spinae and multifidus (deep spinal stabilisers) contract to maintain the backbend. Pectorals and anterior shoulder fascia are placed in a gentle stretch — excellent for sedentary individuals.",
            caloriesBurned = "~2–3 kcal/min",
            muscleEmoji = "🧘",
            estimatedMinutes = 10,
            imageUrl = "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "tree_pose",
            name = "Tree Pose",
            sanskritName = "Vrikshasana",
            category = ExerciseCategory.YOGA,
            primaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.CORE),
            secondaryMuscles = listOf(MuscleGroup.CALVES, MuscleGroup.HIP_FLEXORS),
            equipment = EquipmentType.MAT,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.IMPROVE_FLEXIBILITY, ClientGoal.GENERAL_FITNESS),
            sets = "Hold each side", reps = "30–60 sec per side", tempo = "Static balance", rest = "Between sides",
            howTo = listOf(
                "Stand tall. Shift weight to right foot.",
                "Place left sole on right inner thigh (or calf — never knee).",
                "Hands at heart or overhead. Fix gaze on a point.",
                "Hold. Breathe calmly. Switch sides."
            ),
            commonErrors = listOf("Foot pressing on the knee (unsafe)", "Raised hip on standing leg", "Holding breath affects balance"),
            benefits = listOf("Develops single-leg balance", "Strengthens hip abductors", "Improves proprioception and ankle stability"),
            bodyEffect = "Challenges vestibular system and proprioception. Hip abductors work isometrically to prevent Trendelenburg sign. Improves neuromuscular control — directly reduces injury risk in sports.",
            caloriesBurned = "~2–3 kcal/min",
            muscleEmoji = "🧘",
            estimatedMinutes = 15,
            imageUrl = "https://images.unsplash.com/photo-1510894347713-fc3dc6166086?w=400&h=500&fit=crop&q=80"
        ))

        // ── CARDIO ────────────────────────────────────────────────────────────

        add(Exercise(
            id = "jumping_jacks",
            name = "Jumping Jacks",
            category = ExerciseCategory.CARDIO,
            primaryMuscles = listOf(MuscleGroup.FULL_BODY),
            secondaryMuscles = emptyList(),
            equipment = EquipmentType.BODYWEIGHT,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.LOSE_FAT, ClientGoal.IMPROVE_CARDIO, ClientGoal.GENERAL_FITNESS),
            sets = "3–4 sets", reps = "30–60 sec", tempo = "Moderate pace", rest = "30 sec",
            howTo = listOf(
                "Stand straight, arms at sides.",
                "Jump and simultaneously spread feet and raise arms overhead.",
                "Jump back to start. Land softly on balls of feet.",
                "Keep a consistent rhythmic pace."
            ),
            commonErrors = listOf("Landing with stiff legs — shock through knees", "Partial arm range of motion", "Breath-holding"),
            benefits = listOf("Elevates heart rate quickly", "Low-skill full-body warmup or finisher", "Coordinates bilateral limb movement"),
            bodyEffect = "Rapidly elevates heart rate to 60–80% max HR. Calves and hip abductors are primary movers. Effective for increasing VO₂ max over time when done progressively.",
            caloriesBurned = "~8–12 kcal/min",
            muscleEmoji = "🏃",
            estimatedMinutes = 20,
            imageUrl = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "burpee",
            name = "Burpee",
            category = ExerciseCategory.CARDIO,
            primaryMuscles = listOf(MuscleGroup.FULL_BODY),
            secondaryMuscles = emptyList(),
            equipment = EquipmentType.BODYWEIGHT,
            difficulty = DifficultyLevel.INTERMEDIATE,
            suitableFor = listOf(ClientGoal.LOSE_FAT, ClientGoal.IMPROVE_CARDIO),
            sets = "3–5 sets", reps = "8–15 reps or 30 sec", tempo = "Fast", rest = "60 sec",
            howTo = listOf(
                "Stand. Drop hands to floor, jump feet back to plank.",
                "Perform a push-up.",
                "Jump feet forward between hands. Explode upward.",
                "Jump with arms overhead. Land softly."
            ),
            commonErrors = listOf("Skipping the push-up (reduces upper body work)", "Slamming the ground on landing", "Lower back sagging in plank phase"),
            benefits = listOf("Maximum calorie burn in minimum time", "Combines strength + cardio in one movement", "No equipment needed"),
            bodyEffect = "EPOC (Excess Post-Exercise Oxygen Consumption) is highest with burpees — you continue burning calories for up to 48 hrs after. Heart rate reaches 80–95% max.",
            caloriesBurned = "~12–15 kcal/min",
            muscleEmoji = "⚡",
            estimatedMinutes = 25,
            imageUrl = "https://images.unsplash.com/photo-1549060279-7e168fcee0c2?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "mountain_climber",
            name = "Mountain Climbers",
            category = ExerciseCategory.CARDIO,
            primaryMuscles = listOf(MuscleGroup.CORE, MuscleGroup.HIP_FLEXORS),
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.QUADS),
            equipment = EquipmentType.BODYWEIGHT,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.LOSE_FAT, ClientGoal.IMPROVE_CARDIO, ClientGoal.BUILD_MUSCLE),
            sets = "3 sets", reps = "30–45 sec", tempo = "Fast-paced", rest = "30–45 sec",
            howTo = listOf(
                "Start in high plank. Arms straight under shoulders.",
                "Drive right knee toward chest.",
                "Switch legs rapidly in a running motion.",
                "Keep hips level throughout — don't bounce up."
            ),
            commonErrors = listOf("Hips rising with each drive", "Wrists behind shoulders (not under)", "Slowing too much — loses cardio benefit"),
            benefits = listOf("Combines core work and cardio", "Hip flexor strengthening", "Shoulder stability under load"),
            bodyEffect = "Rectus abdominis and transverse abdominis work to stabilise as hips try to flex. Psoas (hip flexor) under repeated concentric contraction. Heart rate maintained at 70–85% max HR.",
            caloriesBurned = "~9–13 kcal/min",
            muscleEmoji = "🏃",
            estimatedMinutes = 20,
            imageUrl = "https://images.unsplash.com/photo-1601422407692-ec4eeec1d9b3?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "high_knees",
            name = "High Knees",
            category = ExerciseCategory.CARDIO,
            primaryMuscles = listOf(MuscleGroup.HIP_FLEXORS, MuscleGroup.QUADS),
            secondaryMuscles = listOf(MuscleGroup.CORE, MuscleGroup.CALVES),
            equipment = EquipmentType.BODYWEIGHT,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.LOSE_FAT, ClientGoal.IMPROVE_CARDIO),
            sets = "3–4 sets", reps = "30–60 sec", tempo = "Fast", rest = "30 sec",
            howTo = listOf(
                "Stand, core engaged. Begin running in place.",
                "Drive knees to hip height with each step.",
                "Pump arms — opposite arm to leg for coordination.",
                "Land on balls of feet to protect joints."
            ),
            commonErrors = listOf("Knees not reaching hip height (reduces effectiveness)", "Leaning back", "Landing on heels — impact injury risk"),
            benefits = listOf("Elevates HR to aerobic/anaerobic threshold fast", "Strengthens hip flexors", "Improves running mechanics"),
            bodyEffect = "Psoas and iliacus (hip flexors) under maximum concentric load. Gastrocnemius and soleus absorb landing forces. Sustained effort improves VO₂ max and lactate threshold.",
            caloriesBurned = "~10–14 kcal/min",
            muscleEmoji = "🏃",
            estimatedMinutes = 15,
            imageUrl = "https://images.unsplash.com/photo-1594737625785-a6cbdabd333c?w=400&h=500&fit=crop&q=80"
        ))

        // ── HIIT ─────────────────────────────────────────────────────────────

        add(Exercise(
            id = "box_jump",
            name = "Box Jump",
            category = ExerciseCategory.HIIT,
            primaryMuscles = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
            secondaryMuscles = listOf(MuscleGroup.CORE, MuscleGroup.HAMSTRINGS),
            equipment = EquipmentType.BODYWEIGHT,
            difficulty = DifficultyLevel.INTERMEDIATE,
            suitableFor = listOf(ClientGoal.LOSE_FAT, ClientGoal.IMPROVE_CARDIO, ClientGoal.BUILD_MUSCLE),
            sets = "4–5 sets", reps = "5–8 reps", tempo = "Explosive", rest = "60–90 sec",
            howTo = listOf(
                "Stand 30 cm from box. Athletic stance.",
                "Swing arms, hinge hips — countermovement dip.",
                "Explode upward — land softly with knees bent in the middle of the box.",
                "Step down carefully. Reset. Repeat."
            ),
            commonErrors = listOf("Landing with straight knees — knee injury risk", "Starting too close to box", "Not using arm swing — reduces jump height"),
            benefits = listOf("Maximum power output development", "EPOC effect burns fat long after exercise", "Improves reactive strength"),
            bodyEffect = "Stretch-shortening cycle is maximally challenged. Fast-twitch (Type II) muscle fibres are recruited exclusively during the concentric phase. Stimulates neuromuscular adaptations that improve all athletic performance.",
            caloriesBurned = "~12–16 kcal/min",
            muscleEmoji = "⚡",
            estimatedMinutes = 30,
            imageUrl = "https://images.unsplash.com/photo-1550259979-ed79b48d2a30?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "kb_swing",
            name = "Kettlebell Swing",
            category = ExerciseCategory.HIIT,
            primaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            secondaryMuscles = listOf(MuscleGroup.CORE, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
            equipment = EquipmentType.KETTLEBELL,
            difficulty = DifficultyLevel.INTERMEDIATE,
            suitableFor = listOf(ClientGoal.LOSE_FAT, ClientGoal.BUILD_MUSCLE, ClientGoal.IMPROVE_CARDIO),
            sets = "4–5 sets", reps = "15–25 reps", tempo = "Explosive hip hinge", rest = "60–90 sec",
            howTo = listOf(
                "Stand, KB between feet. Hinge at hips, grip handle.",
                "Hike KB back between legs — load the hamstrings.",
                "Drive hips forward explosively — KB floats to shoulder height.",
                "Let it fall back through legs. Absorb with hips, not knees."
            ),
            commonErrors = listOf("Squatting instead of hinging — quad-dominant, not glute-dominant", "Pulling with arms instead of driving with hips", "Losing neutral spine at bottom"),
            benefits = listOf("Develops posterior chain explosively", "High caloric expenditure", "Improves hip power for sports"),
            bodyEffect = "The hip hinge pattern trains glutes and hamstrings for power, not just strength. Metabolic demand is high (similar to sprinting). Core anti-flexion under load builds real-world strength.",
            caloriesBurned = "~13–17 kcal/min",
            muscleEmoji = "⚡",
            estimatedMinutes = 25,
            imageUrl = "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=400&h=500&fit=crop&q=80"
        ))

        // ── FLEXIBILITY ───────────────────────────────────────────────────────

        add(Exercise(
            id = "hip_flexor_stretch",
            name = "Low Lunge Hip Flexor Stretch",
            category = ExerciseCategory.FLEXIBILITY,
            primaryMuscles = listOf(MuscleGroup.HIP_FLEXORS),
            secondaryMuscles = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            equipment = EquipmentType.MAT,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.IMPROVE_FLEXIBILITY, ClientGoal.GENERAL_FITNESS),
            sets = "Each side", reps = "30–60 sec hold", tempo = "Static", rest = "Transition to other side",
            howTo = listOf(
                "Kneel on left knee, right foot forward — 90° front knee.",
                "Shift hips forward until tension felt at front of left hip.",
                "Keep torso upright. Breathe into the stretch.",
                "Option: raise left arm for deeper hip flexor stretch."
            ),
            commonErrors = listOf("Front knee drifting over toes", "Arching lower back to compensate", "Bouncing into the stretch"),
            benefits = listOf("Counteracts sitting-induced hip flexor tightness", "Improves squat and lunge depth", "Reduces anterior pelvic tilt"),
            bodyEffect = "Iliopsoas (psoas major + iliacus) is placed in lengthened position. Chronic tightness causes anterior pelvic tilt and lower back pain. Regular stretching restores neutral pelvic alignment.",
            caloriesBurned = "~2 kcal/min",
            muscleEmoji = "🤸",
            estimatedMinutes = 15,
            imageUrl = "https://images.unsplash.com/photo-1552196563-55cd4e45efb3?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "hamstring_stretch",
            name = "Seated Hamstring Stretch",
            category = ExerciseCategory.FLEXIBILITY,
            primaryMuscles = listOf(MuscleGroup.HAMSTRINGS),
            secondaryMuscles = listOf(MuscleGroup.SPINE),
            equipment = EquipmentType.MAT,
            difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.IMPROVE_FLEXIBILITY, ClientGoal.GENERAL_FITNESS),
            sets = "Each side", reps = "30–45 sec hold", tempo = "Static", rest = "Between sides",
            howTo = listOf(
                "Sit with one leg extended, other bent inward.",
                "Hinge forward from hips — not by rounding spine.",
                "Reach toward foot. Feel stretch in back of thigh.",
                "Breathe out to deepen. No bouncing."
            ),
            commonErrors = listOf("Rounding spine (stretches back, not hamstrings)", "Reaching too hard and holding breath", "Bent knee on stretching leg reduces effectiveness"),
            benefits = listOf("Improves posterior chain flexibility", "Reduces risk of hamstring strains", "Helps squat and deadlift mechanics"),
            bodyEffect = "Biceps femoris, semitendinosus, semimembranosus (hamstring group) undergo lengthening under mild neurological inhibition (autogenic inhibition via Golgi tendon organs). Sustained pressure over 30 sec increases plastic deformation in muscle-tendon unit.",
            caloriesBurned = "~2 kcal/min",
            muscleEmoji = "🤸",
            estimatedMinutes = 15,
            imageUrl = "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=400&h=500&fit=crop&q=80"
        ))

        // ── TEEN (15–17) ─────────────────────────────────────────────────────

        add(Exercise(
            id = "jump_rope", name = "Jump Rope",
            category = ExerciseCategory.CARDIO,
            primaryMuscles = listOf(MuscleGroup.CALVES, MuscleGroup.FULL_BODY),
            secondaryMuscles = listOf(MuscleGroup.CORE, MuscleGroup.SHOULDERS),
            equipment = EquipmentType.BODYWEIGHT, difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.LOSE_FAT, ClientGoal.IMPROVE_CARDIO, ClientGoal.GENERAL_FITNESS),
            ageGroups = listOf(AgeGroup.TEEN, AgeGroup.YOUNG_ADULT),
            sets = "3 rounds", reps = "60–90 sec", tempo = "continuous", rest = "30 sec",
            howTo = listOf("Hold rope handles at hip height.", "Swing rope overhead with wrists — not arms.", "Jump 2–3 cm off ground, land softly on balls of feet.", "Keep core tight and elbows close to body.", "Aim for steady rhythm before increasing speed."),
            commonErrors = listOf("Jumping too high wastes energy", "Using full arm swing — wrist rotation is enough", "Landing on heels — increases shin splint risk"),
            benefits = listOf("Improves coordination and agility", "Burns ~10–15 kcal/min", "Builds cardiovascular endurance quickly", "Develops fast-twitch muscle fibers"),
            bodyEffect = "Elevates heart rate rapidly, engaging the phosphocreatine and glycolytic energy systems. Trains calf, core, and shoulder endurance simultaneously. Neurologically develops timing and proprioception essential for sports.",
            caloriesBurned = "~10–15 kcal/min", muscleEmoji = "⚡", estimatedMinutes = 10,
            imageUrl = "https://images.unsplash.com/photo-1601422407692-ec4eff3466ef?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "bodyweight_squat_teen", name = "Bodyweight Squat (Form Focus)",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
            equipment = EquipmentType.BODYWEIGHT, difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.GENERAL_FITNESS, ClientGoal.BUILD_MUSCLE),
            ageGroups = listOf(AgeGroup.TEEN),
            sets = "3 sets", reps = "15–20 reps", tempo = "2-0-2-0", rest = "45 sec",
            howTo = listOf("Stand feet shoulder-width apart, toes slightly out.", "Keep chest tall and core braced throughout.", "Push hips back, then bend knees — squat to parallel.", "Drive through heels to stand. Squeeze glutes at top.", "Maintain neutral spine — no rounding or arching."),
            commonErrors = listOf("Knees caving inward (valgus collapse)", "Leaning forward excessively", "Not reaching parallel depth", "Rising on toes — heels should stay planted"),
            benefits = listOf("Builds foundational leg strength safely", "Teaches correct squat pattern before adding weight", "Improves hip mobility and ankle flexibility"),
            bodyEffect = "Develops quad, glute, and hamstring strength through full range of motion. Activates stabilizer muscles in ankles, knees, and hips simultaneously. Establishes neuromuscular patterns critical before progressing to barbell work.",
            caloriesBurned = "~5–7 kcal/min", muscleEmoji = "🦵", estimatedMinutes = 15,
            imageUrl = "https://images.unsplash.com/photo-1574680096145-d05b474e2155?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "agility_lateral_shuffle", name = "Lateral Shuffle",
            category = ExerciseCategory.HIIT,
            primaryMuscles = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = listOf(MuscleGroup.CALVES, MuscleGroup.CORE),
            equipment = EquipmentType.BODYWEIGHT, difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.IMPROVE_CARDIO, ClientGoal.GENERAL_FITNESS),
            ageGroups = listOf(AgeGroup.TEEN, AgeGroup.YOUNG_ADULT),
            sets = "4 sets", reps = "20 sec each direction", tempo = "explosive", rest = "20 sec",
            howTo = listOf("Stand in athletic stance — knees bent, hips low.", "Shuffle laterally 4–5 steps right, then 4–5 steps left.", "Stay low — do not stand up between shuffles.", "Keep feet shoulder-width, never let feet cross.", "Drive off outside foot for speed."),
            commonErrors = listOf("Standing up between shuffles — loses agility benefit", "Crossing feet increases trip risk", "Looking down — keep eyes forward"),
            benefits = listOf("Develops lateral quickness for sports", "Improves hip abductor and adductor strength", "Burns calories efficiently in short bursts"),
            bodyEffect = "Trains frontal-plane movement patterns rarely targeted in standard gym exercises. Develops hip abductor and adductor strength critical for injury prevention in running and field sports.",
            caloriesBurned = "~8–12 kcal/min", muscleEmoji = "⚡", estimatedMinutes = 10,
            imageUrl = "https://images.unsplash.com/photo-1517963879433-6ad2b056d712?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "teen_plank_shoulder_tap", name = "Plank Shoulder Tap",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.CORE),
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.CHEST),
            equipment = EquipmentType.BODYWEIGHT, difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.GENERAL_FITNESS, ClientGoal.BUILD_MUSCLE),
            ageGroups = listOf(AgeGroup.TEEN, AgeGroup.YOUNG_ADULT),
            sets = "3 sets", reps = "10 taps per side", tempo = "controlled", rest = "45 sec",
            howTo = listOf("Start in high plank — hands under shoulders.", "Brace core tightly to prevent hip rotation.", "Lift right hand, tap left shoulder, return.", "Alternate sides while keeping hips still.", "Feet can be slightly wider for more stability."),
            commonErrors = listOf("Hips rotating or swaying side to side", "Rushing the movement — control is key", "Sagging lower back"),
            benefits = listOf("Builds anti-rotational core stability", "Develops shoulder stability", "Teaches body control under load"),
            bodyEffect = "Challenges the core in anti-rotation — the muscles must resist the tendency to twist as one arm is lifted. Engages deep stabilizers (transverse abdominis, multifidus) more effectively than standard plank.",
            caloriesBurned = "~4–6 kcal/min", muscleEmoji = "🎯", estimatedMinutes = 12,
            imageUrl = "https://images.unsplash.com/photo-1598971639058-fab3c3109a00?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "teen_yoga_sun_salutation_mini", name = "Sun Salutation (Mini Flow)",
            sanskritName = "Surya Namaskar (Short)",
            category = ExerciseCategory.YOGA,
            primaryMuscles = listOf(MuscleGroup.FULL_BODY),
            secondaryMuscles = listOf(MuscleGroup.SPINE, MuscleGroup.CORE),
            equipment = EquipmentType.MAT, difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.IMPROVE_FLEXIBILITY, ClientGoal.GENERAL_FITNESS),
            ageGroups = listOf(AgeGroup.TEEN, AgeGroup.YOUNG_ADULT, AgeGroup.MIDDLE_AGED),
            sets = "3 rounds", reps = "5 breaths each pose", tempo = "breath-linked", rest = "none",
            howTo = listOf("Stand tall (Tadasana), hands at heart center.", "Inhale — sweep arms overhead (Urdhva Hastasana).", "Exhale — fold forward (Uttanasana).", "Inhale — half lift, flat back (Ardha Uttanasana).", "Step or jump back to plank, lower to floor.", "Inhale — Cobra pose (Bhujangasana).", "Exhale — Downward Dog (Adho Mukha Svanasana) for 5 breaths.", "Walk feet to hands, rise back to standing."),
            commonErrors = listOf("Holding breath — movement should link with breath", "Locking elbows in Cobra — keep slight bend", "Rushing through — slower = more benefit"),
            benefits = listOf("Full body stretch and mild strength in one flow", "Calms nervous system and improves focus for studying", "Builds morning energy and joint mobility"),
            bodyEffect = "Sequential joint loading from spine to hips to shoulders in a flow that activates parasympathetic system. Increases synovial fluid circulation in all major joints.",
            caloriesBurned = "~3–5 kcal/min", muscleEmoji = "🧘", estimatedMinutes = 15,
            imageUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=400&h=500&fit=crop&q=80"
        ))

        // ── MIDDLE-AGED (36–55) ──────────────────────────────────────────────

        add(Exercise(
            id = "resistance_band_row", name = "Resistance Band Seated Row",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.BACK),
            secondaryMuscles = listOf(MuscleGroup.BICEPS, MuscleGroup.SHOULDERS),
            equipment = EquipmentType.RESISTANCE_BAND, difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.BUILD_MUSCLE, ClientGoal.GENERAL_FITNESS),
            ageGroups = listOf(AgeGroup.MIDDLE_AGED, AgeGroup.SENIOR),
            sets = "3 sets", reps = "12–15 reps", tempo = "2-1-2-1", rest = "60 sec",
            howTo = listOf("Sit on floor, legs extended. Loop band around feet.", "Hold both ends, sit tall — neutral spine.", "Pull elbows back, squeezing shoulder blades together.", "Pause 1 sec at peak contraction.", "Slowly return — do not let band snap forward."),
            commonErrors = listOf("Rounding back during pull", "Using momentum — control each rep", "Shrugging shoulders — keep them down and back"),
            benefits = listOf("Corrects desk-posture rounded shoulders", "Joint-friendly — no spinal compression", "Strengthens mid-back essential for back pain prevention"),
            bodyEffect = "Activates rhomboids, trapezius, and rear deltoids — the postural muscles weakened by prolonged sitting. Resistance bands provide accommodating resistance that is gentle on joints while still achieving progressive overload.",
            caloriesBurned = "~4–6 kcal/min", muscleEmoji = "🦾", estimatedMinutes = 15,
            imageUrl = "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "foam_roller_thoracic", name = "Foam Roller Thoracic Extension",
            category = ExerciseCategory.FLEXIBILITY,
            primaryMuscles = listOf(MuscleGroup.SPINE),
            secondaryMuscles = listOf(MuscleGroup.CHEST),
            equipment = EquipmentType.BODYWEIGHT, difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.IMPROVE_FLEXIBILITY, ClientGoal.GENERAL_FITNESS),
            ageGroups = listOf(AgeGroup.MIDDLE_AGED, AgeGroup.SENIOR),
            sets = "2 sets", reps = "5–8 extensions per segment", tempo = "slow", rest = "30 sec",
            howTo = listOf("Place foam roller perpendicular to spine at mid-back.", "Support head with hands, knees bent, feet flat.", "Gently extend over the roller — breathe out.", "Move roller 2 cm up spine, repeat extension.", "Work from lower to upper thoracic (avoid neck/lower back)."),
            commonErrors = listOf("Rolling on lower back — lumbar should not be mobilized this way", "Holding breath — exhale deepens the stretch", "Moving too fast — pause at each segment"),
            benefits = listOf("Reverses desk posture kyphosis", "Reduces upper back and neck pain", "Improves shoulder mobility and overhead reach"),
            bodyEffect = "Applies sustained mechanical pressure to facet joints of thoracic vertebrae, mobilizing stiff segments. Stretches anterior chest tissues shortened by prolonged forward posture.",
            caloriesBurned = "~2 kcal/min", muscleEmoji = "🦴", estimatedMinutes = 10,
            imageUrl = "https://images.unsplash.com/photo-1599901860904-17e6ed7083a0?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "lateral_band_walk", name = "Lateral Band Walk",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.GLUTES),
            secondaryMuscles = listOf(MuscleGroup.QUADS, MuscleGroup.HIP_FLEXORS),
            equipment = EquipmentType.RESISTANCE_BAND, difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.GENERAL_FITNESS, ClientGoal.IMPROVE_FLEXIBILITY),
            ageGroups = listOf(AgeGroup.MIDDLE_AGED, AgeGroup.SENIOR, AgeGroup.YOUNG_ADULT),
            sets = "3 sets", reps = "12 steps each direction", tempo = "controlled", rest = "45 sec",
            howTo = listOf("Place resistance band just above knees.", "Stand in quarter-squat — knees bent, hips back.", "Step sideways with one foot, then bring other foot to meet it.", "Maintain tension in band throughout — do not let feet come together.", "Keep toes forward and back flat."),
            commonErrors = listOf("Letting knees cave inward — defeats the purpose", "Standing up between steps", "Moving too fast — lose stability and control"),
            benefits = listOf("Strengthens glute medius — prevents knee valgus and IT band issues", "Helps with hip stability for running and stairs", "Reduces knee pain in active middle-aged adults"),
            bodyEffect = "Isolates gluteus medius and minimus — hip abductors that stabilize the pelvis during single-leg activities. Weakness here is linked to knee pain, hip pain, and running injuries.",
            caloriesBurned = "~4 kcal/min", muscleEmoji = "🍑", estimatedMinutes = 12,
            imageUrl = "https://images.unsplash.com/photo-1534258936925-c58bed479fcb?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "dumbbell_romanian_deadlift", name = "Dumbbell Romanian Deadlift",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
            secondaryMuscles = listOf(MuscleGroup.BACK, MuscleGroup.CORE),
            equipment = EquipmentType.DUMBBELL, difficulty = DifficultyLevel.INTERMEDIATE,
            suitableFor = listOf(ClientGoal.BUILD_MUSCLE, ClientGoal.LOSE_FAT, ClientGoal.GENERAL_FITNESS),
            ageGroups = listOf(AgeGroup.MIDDLE_AGED, AgeGroup.YOUNG_ADULT),
            sets = "3–4 sets", reps = "10–12 reps", tempo = "3-1-2-0", rest = "75 sec",
            howTo = listOf("Stand holding dumbbells in front of thighs.", "Hinge from hips — push them back behind you.", "Lower dumbbells along shins, back stays flat.", "Feel stretch in hamstrings at bottom.", "Drive hips forward to return — squeeze glutes."),
            commonErrors = listOf("Rounding the lower back — high injury risk", "Bending knees too much — becomes a squat", "Letting dumbbells drift forward away from body"),
            benefits = listOf("Safer than barbell RDL for those with lower back history", "Builds powerful posterior chain", "Improves hip hinge — essential for daily lifting movements"),
            bodyEffect = "Eccentrically loads hamstrings and glutes under tension — the most effective stimulus for hypertrophy in posterior chain. Spinal erectors work isometrically to maintain neutral spine.",
            caloriesBurned = "~6–8 kcal/min", muscleEmoji = "🦵", estimatedMinutes = 20,
            imageUrl = "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "walking_lunge", name = "Walking Lunge",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
            equipment = EquipmentType.BODYWEIGHT, difficulty = DifficultyLevel.INTERMEDIATE,
            suitableFor = listOf(ClientGoal.BUILD_MUSCLE, ClientGoal.LOSE_FAT, ClientGoal.GENERAL_FITNESS),
            ageGroups = listOf(AgeGroup.MIDDLE_AGED, AgeGroup.YOUNG_ADULT),
            sets = "3 sets", reps = "10 steps each leg", tempo = "controlled", rest = "60 sec",
            howTo = listOf("Stand tall with core engaged.", "Step forward with right foot into a lunge.", "Lower left knee toward floor — stop 2cm above ground.", "Push through right heel to step forward with left leg.", "Alternate legs walking forward — keep torso upright."),
            commonErrors = listOf("Front knee going past toes — increases joint stress", "Leaning torso forward", "Short stride — reduces range of motion"),
            benefits = listOf("Unilateral training corrects strength imbalances", "Improves single-leg stability and balance", "Functional movement pattern for daily activities"),
            bodyEffect = "Each lunge step loads one leg at a time, revealing and correcting left-right strength imbalances. Demands greater core stabilization than bilateral squats due to the rotational forces from stepping.",
            caloriesBurned = "~6–8 kcal/min", muscleEmoji = "🦵", estimatedMinutes = 15,
            imageUrl = "https://images.unsplash.com/photo-1434682881908-b43d0467b798?w=400&h=500&fit=crop&q=80"
        ))

        // ── SENIOR (56+) ─────────────────────────────────────────────────────

        add(Exercise(
            id = "chair_squat", name = "Chair Squat",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaryMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
            equipment = EquipmentType.BODYWEIGHT, difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.GENERAL_FITNESS, ClientGoal.BUILD_MUSCLE),
            ageGroups = listOf(AgeGroup.SENIOR),
            sets = "3 sets", reps = "10–12 reps", tempo = "slow and controlled", rest = "60 sec",
            howTo = listOf("Stand in front of a sturdy chair, feet shoulder-width.", "Arms extended forward for balance.", "Slowly lower yourself toward the chair seat.", "Tap the seat gently — do not sit fully.", "Drive through heels to stand back up."),
            commonErrors = listOf("Dropping into the chair instead of controlled lower", "Knees caving inward", "Leaning too far forward"),
            benefits = listOf("Directly trains the sit-to-stand movement — critical for independence", "Builds quad and glute strength safely", "Reduces fall risk by improving lower body strength"),
            bodyEffect = "Trains the exact motor pattern needed to rise from a chair — the most common cause of falls and loss of independence in seniors. The chair provides a safety stop at the end range of motion.",
            caloriesBurned = "~3–4 kcal/min", muscleEmoji = "🦵", estimatedMinutes = 12,
            imageUrl = "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "wall_push_up", name = "Wall Push-Up",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.CORE),
            equipment = EquipmentType.BODYWEIGHT, difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.GENERAL_FITNESS, ClientGoal.BUILD_MUSCLE),
            ageGroups = listOf(AgeGroup.SENIOR),
            sets = "2–3 sets", reps = "10–15 reps", tempo = "2-1-2-0", rest = "60 sec",
            howTo = listOf("Stand arm's length from wall. Place hands at shoulder height.", "Keep body in straight line from head to heels.", "Bend elbows to lower chest toward wall.", "Push back to start — fully extend arms.", "The further from the wall, the harder the exercise."),
            commonErrors = listOf("Letting hips sag forward", "Flaring elbows wide — keep them at 45°", "Using momentum instead of controlled movement"),
            benefits = listOf("Builds upper body pushing strength with zero joint risk", "No floor-to-ground requirement — easier for seniors", "Foundation for progressing to incline then floor push-ups"),
            bodyEffect = "Provides chest, shoulder, and tricep stimulus at low loading compared to floor push-up. The reduced angle makes it appropriate for those with wrist, shoulder, or elbow limitations while still creating muscular stimulus.",
            caloriesBurned = "~3 kcal/min", muscleEmoji = "💪", estimatedMinutes = 10,
            imageUrl = "https://images.unsplash.com/photo-1598971639058-fab3c3109a00?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "seated_leg_raise", name = "Seated Leg Raise",
            category = ExerciseCategory.STRENGTH,
            primaryMuscles = listOf(MuscleGroup.QUADS, MuscleGroup.HIP_FLEXORS),
            secondaryMuscles = listOf(MuscleGroup.CORE),
            equipment = EquipmentType.BODYWEIGHT, difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.GENERAL_FITNESS),
            ageGroups = listOf(AgeGroup.SENIOR),
            sets = "3 sets", reps = "10–12 per leg", tempo = "2-2-2-1", rest = "45 sec",
            howTo = listOf("Sit tall in a chair, both feet flat on floor.", "Grip armrests or chair sides for stability.", "Slowly lift right knee until leg is extended horizontal.", "Hold 2 seconds — feel quadriceps contract.", "Slowly lower. Complete set, then switch legs."),
            commonErrors = listOf("Leaning back as leg rises — use core", "Swinging leg up — should be controlled lift", "Dropping leg quickly — the lowering is half the exercise"),
            benefits = listOf("Strengthens quads without any floor work", "Improves knee extension strength — key for stair climbing", "Reduces knee pain over time with consistent practice"),
            bodyEffect = "Isometrically strengthens the vastus medialis (inner quad) which supports the knee cap and prevents patellar tracking issues common in seniors. The seated position eliminates balance demands so full focus goes to the muscle.",
            caloriesBurned = "~2–3 kcal/min", muscleEmoji = "🦵", estimatedMinutes = 10,
            imageUrl = "https://images.unsplash.com/photo-1599901860904-17e6ed7083a0?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "balance_single_leg_stand", name = "Single Leg Balance Stand",
            category = ExerciseCategory.FLEXIBILITY,
            primaryMuscles = listOf(MuscleGroup.CALVES, MuscleGroup.CORE),
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.HIP_FLEXORS),
            equipment = EquipmentType.BODYWEIGHT, difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.GENERAL_FITNESS, ClientGoal.IMPROVE_FLEXIBILITY),
            ageGroups = listOf(AgeGroup.SENIOR, AgeGroup.MIDDLE_AGED),
            sets = "3 sets", reps = "20–30 sec each leg", tempo = "static hold", rest = "30 sec",
            howTo = listOf("Stand near a wall or chair for safety — do not grip unless needed.", "Lift one foot a few centimeters off floor.", "Find a focal point to help balance.", "Hold steady — breathe normally.", "Progress by closing eyes or standing on a folded towel."),
            commonErrors = listOf("Gripping support — reduces proprioceptive challenge", "Locking standing knee — keep slight bend", "Looking around — maintain a fixed gaze point"),
            benefits = listOf("Most effective single exercise for fall prevention", "Strengthens ankle stabilizers and proprioceptors", "Directly trains the balance system that declines with age"),
            bodyEffect = "Challenges the vestibular, visual, and proprioceptive systems simultaneously. Activates small stabilizing muscles in the foot, ankle, and hip that are rarely trained but critical for preventing falls.",
            caloriesBurned = "~2 kcal/min", muscleEmoji = "⚖️", estimatedMinutes = 8,
            imageUrl = "https://images.unsplash.com/photo-1549060279-7e168fcee0c2?w=400&h=500&fit=crop&q=80"
        ))

        add(Exercise(
            id = "cat_cow_senior", name = "Cat-Cow Spinal Mobility",
            sanskritName = "Marjaryasana-Bitilasana",
            category = ExerciseCategory.YOGA,
            primaryMuscles = listOf(MuscleGroup.SPINE),
            secondaryMuscles = listOf(MuscleGroup.CORE, MuscleGroup.BACK),
            equipment = EquipmentType.MAT, difficulty = DifficultyLevel.BEGINNER,
            suitableFor = listOf(ClientGoal.IMPROVE_FLEXIBILITY, ClientGoal.GENERAL_FITNESS),
            ageGroups = listOf(AgeGroup.SENIOR, AgeGroup.MIDDLE_AGED),
            sets = "2–3 rounds", reps = "10 cycles", tempo = "breath-linked", rest = "none",
            howTo = listOf("Start on hands and knees — wrists under shoulders, knees under hips.", "COW: Inhale — drop belly, lift chest and tailbone.", "CAT: Exhale — round spine toward ceiling, tuck chin and tailbone.", "Move slowly and link each movement to one full breath.", "Feel each vertebra move sequentially."),
            commonErrors = listOf("Moving too fast — lose the breath-movement link", "Only moving the extreme of the spine — try to articulate each segment", "Wrist pain — use fists or forearms as modification"),
            benefits = listOf("Lubricates spinal discs with synovial fluid", "Reduces morning back stiffness immediately", "Gentle enough for daily use with zero injury risk"),
            bodyEffect = "The alternating flexion and extension pumps synovial fluid into intervertebral discs — the avascular structures that receive nutrition only through movement. Activates deep spinal extensors and multifidus.",
            caloriesBurned = "~2 kcal/min", muscleEmoji = "🦴", estimatedMinutes = 8,
            imageUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=400&h=500&fit=crop&q=80"
        ))
    }

    fun byCategory(category: ExerciseCategory) = all.filter { it.category == category }
    fun byGoal(goal: ClientGoal) = all.filter { goal in it.suitableFor }
    fun byMuscle(muscle: MuscleGroup) = all.filter { muscle in it.primaryMuscles || muscle in it.secondaryMuscles }
    fun byId(id: String) = all.find { it.id == id }
    fun byAgeGroup(group: AgeGroup) = all.filter { group in it.ageGroups }
}
