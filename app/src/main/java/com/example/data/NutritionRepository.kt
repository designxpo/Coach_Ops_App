package com.example.data

object NutritionRepository {

    private fun item(name: String, qty: String, cal: Int, pro: Int, carb: Int, fat: Int, benefit: String, isVeg: Boolean = true) =
        IndianFoodItem(name, qty, cal, pro, carb, fat, benefit, isVeg)

    val plans: List<IndianMealPlan> = listOf(

        // ── BUILD MUSCLE ──────────────────────────────────────────────────────
        IndianMealPlan(
            goal = ClientGoal.BUILD_MUSCLE,
            dailyCalories = 2800,
            proteinG = 160,
            carbsG = 320,
            fatG = 80,
            hydration = "3.5–4 L water/day. Add electrolytes on training days.",
            generalTips = listOf(
                "Eat every 3–4 hours to keep amino-acid levels elevated.",
                "Have a protein source at every meal — paneer, eggs, dal, or chicken.",
                "Do not skip the pre- and post-workout meals.",
                "Whole foods first. Protein powder is a supplement — not a meal."
            ),
            meals = listOf(
                IndianMeal("Pre-Workout Snack", "6:00 – 7:00 AM", listOf(
                    item("Banana", "1 large", 90, 1, 23, 0, "Fast-digesting carbs for instant workout energy"),
                    item("Peanut Butter", "1 tbsp", 95, 4, 3, 8, "Healthy fats + protein for sustained energy"),
                    item("Low-fat Milk", "200 ml", 70, 7, 10, 1, "Casein protein keeps you full through workout")
                )),
                IndianMeal("Breakfast", "8:00 – 9:00 AM", listOf(
                    item("Paneer Paratha (whole wheat)", "2 parathas", 420, 18, 54, 14, "High protein + complex carbs for muscle fuel"),
                    item("Dahi (curd)", "1 cup / 150 g", 90, 8, 10, 2, "Probiotics for gut health; casein for slow protein release"),
                    item("Whole Eggs", "3 eggs", 210, 18, 2, 15, "Complete amino-acid profile; highest biological value protein", false)
                )),
                IndianMeal("Mid-Morning", "11:00 AM", listOf(
                    item("Chana Chaat", "1 cup", 180, 10, 28, 3, "Chickpeas — slow carb + fibre + plant protein"),
                    item("Nimbu Paani (no sugar)", "1 glass", 10, 0, 3, 0, "Electrolytes + Vitamin C")
                )),
                IndianMeal("Lunch", "1:00 – 2:00 PM", listOf(
                    item("Cooked Rice (white)", "1.5 cups", 320, 6, 70, 1, "Fast carbs for post-workout glycogen replenishment"),
                    item("Masoor Dal Tadka", "1 bowl / 200 g", 200, 14, 30, 4, "Lentils: complete plant protein when combined with rice"),
                    item("Chicken Curry (breast)", "150 g chicken", 250, 38, 6, 9, "Lean protein for muscle protein synthesis", false),
                    item("Palak Sabzi", "1 cup", 80, 5, 10, 3, "Iron + folate for red blood cell production and oxygen delivery"),
                    item("Mixed Salad with Lemon", "1 plate", 50, 2, 8, 1, "Micronutrients + enzymes for digestion")
                )),
                IndianMeal("Post-Workout", "5:00 PM", listOf(
                    item("Sabudana (tapioca pearls) Khichdi", "1 bowl", 250, 4, 55, 3, "Fast carbs to spike insulin and drive glucose into muscles"),
                    item("Whey Protein Shake", "1 scoop / 200 ml milk", 250, 30, 18, 4, "Fast-absorbing whey to maximise MPS (muscle protein synthesis) in the anabolic window", false)
                )),
                IndianMeal("Dinner", "8:00 – 9:00 PM", listOf(
                    item("Whole Wheat Roti", "3 rotis", 270, 9, 54, 4, "Complex carbs + fibre"),
                    item("Rajma Curry", "1 cup", 210, 13, 34, 4, "Kidney beans — plant protein + slow carb"),
                    item("Grilled Paneer Tikka", "100 g", 260, 18, 5, 18, "High protein, medium fat — ideal for overnight recovery"),
                    item("Cucumber Raita", "1 cup", 80, 4, 8, 3, "Probiotics + cooling digestive aid")
                )),
                IndianMeal("Before Bed", "10:30 PM", listOf(
                    item("Warm Turmeric Milk (haldi doodh)", "250 ml", 150, 8, 12, 6, "Casein protein for 7-hr slow release; curcumin reduces DOMS (delayed onset muscle soreness)")
                ))
            )
        ),

        // ── LOSE FAT ─────────────────────────────────────────────────────────
        IndianMealPlan(
            goal = ClientGoal.LOSE_FAT,
            dailyCalories = 1700,
            proteinG = 130,
            carbsG = 160,
            fatG = 55,
            hydration = "3–4 L water/day. Warm water before meals suppresses appetite.",
            generalTips = listOf(
                "Eat protein first at every meal — it signals satiety hormones (GLP-1, PYY).",
                "Prefer complex carbs (brown rice, whole wheat) over refined ones.",
                "Cook in minimal oil — use non-stick or air-frying techniques.",
                "Do not eat carbs late at night — insulin sensitivity is lower in the evening."
            ),
            meals = listOf(
                IndianMeal("Early Morning (empty stomach)", "6:00 – 7:00 AM", listOf(
                    item("Warm Water + Lemon + Jeera (cumin)", "1 glass", 10, 0, 2, 0, "Boosts metabolism, suppresses appetite, aids liver detox"),
                    item("Soaked Methi (fenugreek seeds)", "1 tsp soaked", 15, 1, 2, 0, "Slows glucose absorption, reduces insulin spikes")
                )),
                IndianMeal("Breakfast", "8:00 – 9:00 AM", listOf(
                    item("Moong Dal Chilla (green gram pancake)", "3 chillas", 250, 16, 32, 4, "High protein, low fat breakfast — keeps you full 4+ hours"),
                    item("Green Chutney (mint + coriander)", "2 tbsp", 20, 1, 3, 0, "Antioxidants + zero-calorie flavour"),
                    item("Boiled Egg Whites", "3 whites", 50, 11, 0, 0, "Pure protein with zero fat", false)
                )),
                IndianMeal("Mid-Morning", "11:00 AM", listOf(
                    item("Green Tea", "1 cup", 5, 0, 1, 0, "EGCG catechins increase fat oxidation by ~17%"),
                    item("Handful Roasted Makhana (fox nuts)", "30 g", 100, 4, 19, 1, "Low GI snack — prevents energy crashes")
                )),
                IndianMeal("Lunch", "1:00 PM", listOf(
                    item("Brown Rice", "¾ cup cooked", 160, 3, 34, 1, "Lower GI than white rice; more fibre"),
                    item("Chana Dal (split chickpeas)", "1 bowl", 175, 12, 26, 4, "High satiety, high fibre, medium protein"),
                    item("Palak (spinach) Sabzi", "1 cup", 80, 5, 10, 3, "Iron + folate; very low calorie density"),
                    item("Raita with cucumber", "1 cup", 80, 5, 8, 2, "Probiotics improve fat metabolism")
                )),
                IndianMeal("Afternoon Snack", "4:00 PM", listOf(
                    item("Mixed Sprouts (moong, chana, rajma)", "1 cup", 120, 9, 18, 1, "Sprouting increases enzyme activity + protein bioavailability"),
                    item("Lemon + Black Pepper seasoning", "1 tsp", 5, 0, 1, 0, "Piperine in black pepper enhances curcumin absorption by 2000%")
                )),
                IndianMeal("Dinner (light)", "7:00 – 8:00 PM", listOf(
                    item("2 Jowar / Bajra Roti", "2 rotis", 180, 6, 36, 2, "Millets: high fibre + magnesium; much better than maida"),
                    item("Mixed Vegetable Sabzi (no potato)", "1 cup", 100, 4, 14, 3, "Micronutrient-dense, low calorie"),
                    item("Clear Soup (tomato / dal)", "1 bowl", 60, 3, 9, 1, "Broth signals satiety with almost zero calories")
                )),
                IndianMeal("Post-Dinner (optional)", "9:30 PM", listOf(
                    item("Warm Jeera Water or Chamomile Tea", "1 cup", 5, 0, 1, 0, "Reduces bloating; chamomile lowers cortisol (fat-storing hormone)")
                ))
            )
        ),

        // ── IMPROVE CARDIO ────────────────────────────────────────────────────
        IndianMealPlan(
            goal = ClientGoal.IMPROVE_CARDIO,
            dailyCalories = 2200,
            proteinG = 110,
            carbsG = 290,
            fatG = 65,
            hydration = "4+ L water/day. Drink 500 ml 2 hours before cardio.",
            generalTips = listOf(
                "Carbs are your primary cardio fuel — don't restrict them.",
                "Iron-rich foods prevent anaemia which kills cardio performance.",
                "Eat 1–2 hours before long cardio sessions. Never train fasted for long runs.",
                "Banana + jaggery is an excellent intra-workout natural energy source."
            ),
            meals = listOf(
                IndianMeal("Pre-Cardio", "6:30 AM", listOf(
                    item("2 Bananas", "2 medium", 180, 2, 46, 0, "Fast + medium GI carbs — perfect 60-minute training fuel"),
                    item("Coconut Water", "200 ml", 45, 0, 10, 0, "Natural electrolytes (potassium) pre-workout")
                )),
                IndianMeal("Post-Cardio Breakfast", "9:00 AM", listOf(
                    item("Poha (flattened rice) with veggies", "1.5 cups", 300, 8, 56, 5, "Fast-replenishing carb with micronutrients"),
                    item("Boiled Egg", "2 whole eggs", 140, 12, 1, 10, "Complete protein for post-workout repair", false),
                    item("Fresh Orange Juice", "150 ml", 70, 1, 16, 0, "Vitamin C + carbs; enhances iron absorption from eggs")
                )),
                IndianMeal("Lunch", "1:00 PM", listOf(
                    item("White Rice", "1.5 cups", 320, 6, 70, 1, "Rapid glycogen restoration after morning training"),
                    item("Arhar / Toor Dal", "1 bowl", 190, 12, 28, 4, "Protein + carbs + iron for RBC production"),
                    item("Baingan Bharta or Any Sabzi", "1 cup", 100, 4, 14, 4, "Diverse micronutrients for immune function"),
                    item("Curd", "1 cup", 90, 8, 10, 2, "Probiotics for gut health; improves nutrient absorption")
                )),
                IndianMeal("Snack", "4:00 PM", listOf(
                    item("Ragi (finger millet) Roti", "2 small", 180, 5, 37, 2, "Calcium + complex carbs for bone density and sustained energy"),
                    item("Jaggery (gur)", "10 g", 40, 0, 10, 0, "Natural sugar + iron — better than refined sugar")
                )),
                IndianMeal("Dinner", "8:00 PM", listOf(
                    item("Whole Wheat Roti", "2 rotis", 180, 6, 36, 3, "Complex carbs for overnight glycogen restoration"),
                    item("Chicken Curry or Rajma", "150 g / 1 cup", 230, 30, 8, 9, "High protein for muscle repair", false),
                    item("Palak Dal", "1 cup", 140, 10, 20, 3, "Iron (non-haem) + protein; add lemon for better absorption")
                ))
            )
        ),

        // ── IMPROVE FLEXIBILITY ───────────────────────────────────────────────
        IndianMealPlan(
            goal = ClientGoal.IMPROVE_FLEXIBILITY,
            dailyCalories = 1900,
            proteinG = 90,
            carbsG = 240,
            fatG = 65,
            hydration = "3.5 L water/day. Dehydration directly reduces muscle elasticity.",
            generalTips = listOf(
                "Anti-inflammatory foods are key — turmeric, ginger, omega-3s.",
                "Magnesium-rich foods reduce muscle cramps and support flexibility.",
                "Avoid highly processed foods — they increase systemic inflammation.",
                "Collagen-boosting foods (amla, bone broth) support joint health."
            ),
            meals = listOf(
                IndianMeal("Morning", "7:00 AM", listOf(
                    item("Amla (Indian gooseberry) juice or raw", "1 amla / 30 ml juice", 20, 0, 5, 0, "Highest natural Vitamin C — essential for collagen synthesis"),
                    item("Soaked Almonds", "6–8 almonds", 70, 3, 3, 6, "Magnesium + Vitamin E — reduces muscle soreness and stiffness")
                )),
                IndianMeal("Breakfast", "8:30 AM", listOf(
                    item("Oats Upma with vegetables", "1 bowl", 280, 10, 46, 6, "Oats: high magnesium, anti-inflammatory beta-glucan"),
                    item("Turmeric + Ginger Tea", "1 cup", 15, 0, 3, 0, "Curcumin + gingerol — potent natural anti-inflammatories")
                )),
                IndianMeal("Lunch", "1:00 PM", listOf(
                    item("Khichdi (rice + moong dal)", "1 large bowl", 320, 14, 56, 5, "Easily digestible; high magnesium; tryptophan for relaxation"),
                    item("Dahi", "1 cup", 90, 8, 10, 2, "Calcium for bone health; probiotics for gut"),
                    item("Stir-fried Palak + Methi", "1 cup", 90, 6, 12, 2, "Magnesium + folate + iron")
                )),
                IndianMeal("Dinner", "7:30 PM", listOf(
                    item("2 Rotis", "2 whole wheat", 180, 6, 36, 3, "Complex carbs for gentle energy"),
                    item("Mixed Dal (5-lentil dal)", "1 bowl", 200, 13, 30, 4, "Complete plant amino acids + magnesium"),
                    item("Beetroot Raita", "1 cup", 100, 5, 14, 2, "Nitrates in beets improve circulation to muscles — enhances flexibility")
                ))
            )
        ),

        // ── GENERAL FITNESS ───────────────────────────────────────────────────
        IndianMealPlan(
            goal = ClientGoal.GENERAL_FITNESS,
            dailyCalories = 2000,
            proteinG = 100,
            carbsG = 240,
            fatG = 65,
            hydration = "3 L water/day minimum.",
            generalTips = listOf(
                "Eat the rainbow — different coloured vegetables each day.",
                "Balanced macro split: 50% carbs, 20% protein, 30% fat.",
                "Avoid skipping breakfast — sets metabolic tone for the day.",
                "Limit fried snacks. Choose roasted, boiled, or air-fried alternatives."
            ),
            meals = listOf(
                IndianMeal("Breakfast", "8:00 AM", listOf(
                    item("Idli (2–3) + Sambar", "2 idli + 1 bowl sambar", 290, 12, 52, 4, "Fermented rice-lentil: probiotics + complete protein when combined"),
                    item("Coconut Chutney", "2 tbsp", 60, 1, 3, 5, "Healthy MCT fats from coconut support energy"),
                    item("Filter Coffee / Chai (low sugar)", "1 cup", 60, 2, 8, 2, "Moderate caffeine improves alertness and exercise performance")
                )),
                IndianMeal("Lunch", "1:00 PM", listOf(
                    item("Roti (2–3 whole wheat)", "2 rotis", 220, 7, 44, 3, "Whole wheat: fibre + B vitamins"),
                    item("Mixed Vegetable Curry", "1 bowl", 140, 5, 22, 4, "Diverse micronutrients"),
                    item("Moong or Masoor Dal", "1 cup", 170, 12, 25, 3, "Protein + iron for energy"),
                    item("Salad with lemon dressing", "1 plate", 60, 3, 8, 1, "Digestive enzymes + Vitamin C")
                )),
                IndianMeal("Evening Snack", "4:30 PM", listOf(
                    item("Chana Jor Garam or Murmura Bhel", "1 cup", 130, 6, 22, 2, "Protein snack; low oil version"),
                    item("Buttermilk (chaas)", "1 glass", 70, 5, 8, 2, "Probiotics + hydration + electrolytes")
                )),
                IndianMeal("Dinner", "8:00 PM", listOf(
                    item("Brown Rice or Bajra Roti", "1 cup / 2 roti", 250, 6, 50, 3, "Complex carb for steady energy"),
                    item("Palak Paneer or Dal Makhani", "1 cup", 220, 12, 18, 12, "Protein + iron + calcium"),
                    item("Dahi", "1 small cup", 70, 5, 8, 2, "Gut health + protein")
                ))
            )
        )
    )

    fun forGoal(goal: ClientGoal) = plans.find { it.goal == goal }
}
