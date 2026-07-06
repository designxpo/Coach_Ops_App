package com.example.data

import kotlin.math.roundToInt

/**
 * On-device food nutrition parser. Zero internet required.
 * Fixes: gram parsing, alias collision (moong dal vs dal double-count).
 * 200+ Indian + everyday food entries.
 */
object LocalFoodParser {

    data class FoodEntry(
        val names: List<String>,
        val servingLabel: String,
        val servingGrams: Int,
        val caloriesPer100g: Int,
        val proteinPer100g: Float,
        val carbsPer100g: Float,
        val fatPer100g: Float,
        val fiberPer100g: Float = 0f
    )

    private val WORD_TO_NUM = mapOf(
        "half" to 0.5, "one" to 1.0, "a" to 1.0, "an" to 1.0,
        "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0,
        "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0, "ten" to 10.0,
        "couple" to 2.0, "few" to 3.0, "small" to 0.75, "large" to 1.5, "big" to 1.5,
        "medium" to 1.0, "full" to 1.0, "bowl" to 1.0, "plate" to 1.0,
        "glass" to 1.0, "cup" to 1.0, "katori" to 1.0, "piece" to 1.0,
        "slice" to 1.0, "handful" to 1.0, "scoop" to 1.0
    )

    // ─────────────────────────────────────────────────────────────────────────
    // FOOD DATABASE  (servingGrams = 1 standard Indian portion)
    // ─────────────────────────────────────────────────────────────────────────

    private val DATABASE: List<FoodEntry> = listOf(

        // ── Indian Breads ──────────────────────────────────────────────────
        FoodEntry(listOf("tandoori roti"), "1 tandoori roti", 60, 252, 9f, 49f, 3.5f, 3f),
        FoodEntry(listOf("missi roti", "besan roti"), "1 missi roti", 50, 285, 10f, 45f, 6f, 4f),
        FoodEntry(listOf("makki ki roti", "makki roti", "corn roti"), "1 roti", 50, 218, 5f, 45f, 2.5f, 4f),
        FoodEntry(listOf("bajra roti", "bajre ki roti"), "1 roti", 50, 210, 6f, 42f, 2f, 5f),
        FoodEntry(listOf("methi thepla", "thepla"), "1 thepla", 45, 290, 8f, 42f, 10f, 4f),
        FoodEntry(listOf("lachha paratha"), "1 paratha", 80, 340, 7f, 43f, 16f, 2.5f),
        FoodEntry(listOf("aloo paratha"), "1 paratha", 110, 310, 7f, 45f, 12f, 3f),
        FoodEntry(listOf("paneer paratha"), "1 paratha", 100, 330, 10f, 42f, 14f, 2.5f),
        FoodEntry(listOf("gobi paratha", "cauliflower paratha"), "1 paratha", 100, 295, 7.5f, 43f, 11f, 3f),
        FoodEntry(listOf("paratha", "plain paratha"), "1 paratha", 80, 326, 7.3f, 44f, 14f, 2.5f),
        FoodEntry(listOf("garlic naan"), "1 naan", 90, 330, 9f, 51f, 10f, 2f),
        FoodEntry(listOf("butter naan"), "1 naan", 90, 340, 8.5f, 50f, 12f, 1.8f),
        FoodEntry(listOf("naan"), "1 naan", 90, 317, 8.7f, 50f, 9f, 1.8f),
        FoodEntry(listOf("amritsari kulcha", "stuffed kulcha"), "1 kulcha", 100, 320, 8.5f, 48f, 11f, 2.5f),
        FoodEntry(listOf("kulcha"), "1 kulcha", 80, 310, 8f, 48f, 9f, 2f),
        FoodEntry(listOf("bhatura", "batura"), "1 bhatura", 100, 340, 7f, 46f, 14f, 2f),
        FoodEntry(listOf("puri", "poori"), "1 puri", 35, 375, 6.5f, 45f, 19f, 2f),
        FoodEntry(listOf("roti", "chapati", "chapatti", "phulka", "wheat roti"), "1 roti", 40, 265, 8f, 53f, 3.7f, 3f),
        FoodEntry(listOf("masala dosa"), "1 masala dosa", 200, 180, 5f, 30f, 5f, 2f),
        FoodEntry(listOf("dosa", "plain dosa", "dosai"), "1 dosa", 85, 168, 4.5f, 28f, 4.5f, 1f),
        FoodEntry(listOf("uttapam", "uthappam"), "1 uttapam", 100, 145, 4.8f, 25f, 3.2f, 1.5f),
        FoodEntry(listOf("idli", "idly"), "1 idli", 60, 116, 3.5f, 23f, 0.8f, 1f),
        FoodEntry(listOf("appam"), "1 appam", 70, 130, 3f, 26f, 1.5f, 1f),
        FoodEntry(listOf("pesarattu"), "1 pesarattu", 80, 140, 6f, 24f, 2.5f, 3f),
        FoodEntry(listOf("brown bread", "whole wheat bread", "multigrain bread"), "1 slice", 30, 247, 10f, 45f, 3.5f, 4.5f),
        FoodEntry(listOf("bread", "white bread", "sandwich bread", "toast"), "1 slice", 30, 265, 9f, 49f, 3.2f, 2.7f),

        // ── Rice Dishes ────────────────────────────────────────────────────
        FoodEntry(listOf("chicken biryani"), "1 plate", 300, 175, 13f, 22f, 4.5f, 1f),
        FoodEntry(listOf("mutton biryani"), "1 plate", 300, 185, 12f, 22f, 6f, 1f),
        FoodEntry(listOf("veg biryani", "vegetable biryani"), "1 plate", 300, 145, 5f, 26f, 3.5f, 2f),
        FoodEntry(listOf("biryani", "hyderabadi biryani", "dum biryani"), "1 plate", 300, 156, 8f, 22f, 4.5f, 1f),
        FoodEntry(listOf("jeera rice", "jeera pulao"), "1 katori", 150, 152, 3f, 28f, 4f, 0.8f),
        FoodEntry(listOf("veg pulao", "vegetable pulao", "pulao", "pulav"), "1 katori", 150, 148, 3.5f, 26f, 3.5f, 1.2f),
        FoodEntry(listOf("egg fried rice"), "1 plate", 200, 178, 7f, 27f, 5f, 1f),
        FoodEntry(listOf("chicken fried rice"), "1 plate", 200, 190, 10f, 26f, 5.5f, 1f),
        FoodEntry(listOf("fried rice", "veg fried rice"), "1 plate", 200, 163, 4.5f, 27f, 4.5f, 1f),
        FoodEntry(listOf("curd rice", "dahi rice", "thayir sadam"), "1 katori", 200, 105, 4f, 18f, 2f, 0.5f),
        FoodEntry(listOf("lemon rice", "chitranna"), "1 katori", 150, 140, 3f, 26f, 3.5f, 1f),
        FoodEntry(listOf("dal khichdi", "khichdi", "khichri"), "1 katori", 200, 124, 5f, 22f, 2.5f, 2f),
        FoodEntry(listOf("ven pongal", "pongal"), "1 katori", 200, 143, 4.5f, 24f, 4f, 1.5f),
        FoodEntry(listOf("brown rice"), "1 katori", 150, 123, 2.7f, 26f, 1f, 1.8f),
        FoodEntry(listOf("rice", "white rice", "steamed rice", "plain rice", "boiled rice", "cooked rice"), "1 katori", 150, 130, 2.7f, 28f, 0.3f, 0.4f),

        // ── Dal & Lentils ──────────────────────────────────────────────────
        FoodEntry(listOf("dal makhani", "maa ki dal", "black dal"), "1 katori", 150, 130, 7f, 16f, 5f, 4f),
        FoodEntry(listOf("moong dal", "mung dal", "yellow moong", "green moong"), "1 katori", 150, 105, 7.6f, 18f, 0.7f, 4f),
        FoodEntry(listOf("masoor dal", "masoor", "red lentil", "red dal"), "1 katori", 150, 116, 9f, 20f, 0.4f, 3.5f),
        FoodEntry(listOf("chana dal", "bengal gram dal"), "1 katori", 150, 150, 8f, 24f, 2f, 5f),
        FoodEntry(listOf("urad dal", "black gram dal"), "1 katori", 150, 130, 9f, 20f, 2f, 4f),
        FoodEntry(listOf("toor dal", "arhar dal", "yellow dal", "dal fry", "dal", "daal"), "1 katori", 150, 87, 5.4f, 14f, 1.5f, 3f),
        FoodEntry(listOf("chole", "chana", "chickpea", "kabuli chana"), "1 katori", 150, 164, 8.9f, 27f, 2.6f, 7.6f),
        FoodEntry(listOf("rajma", "kidney bean", "rajmah"), "1 katori", 150, 140, 8.7f, 22f, 2f, 6.4f),
        FoodEntry(listOf("sambar", "sambhar"), "1 katori", 200, 55, 2.8f, 8.5f, 1.2f, 2.5f),
        FoodEntry(listOf("rasam", "tomato rasam"), "1 katori", 200, 25, 1f, 4f, 0.5f, 1f),
        FoodEntry(listOf("kadhi pakora", "kadhi", "besan kadhi"), "1 katori", 200, 98, 4f, 12f, 4f, 1.5f),

        // ── Vegetables & Curries ───────────────────────────────────────────
        FoodEntry(listOf("palak paneer", "saag paneer"), "1 katori", 150, 180, 10f, 8f, 13f, 2f),
        FoodEntry(listOf("matar paneer", "peas paneer"), "1 katori", 150, 190, 9f, 12f, 13f, 3f),
        FoodEntry(listOf("paneer butter masala", "butter paneer"), "1 katori", 150, 215, 11f, 9f, 17f, 1f),
        FoodEntry(listOf("shahi paneer"), "1 katori", 150, 245, 11f, 10f, 19f, 1f),
        FoodEntry(listOf("paneer", "paneer curry"), "1 katori", 150, 210, 10f, 8f, 16f, 1f),
        FoodEntry(listOf("pav bhaji", "pavbhaji"), "1 plate", 250, 220, 6f, 32f, 8f, 4f),
        FoodEntry(listOf("aloo gobi", "potato cauliflower"), "1 katori", 150, 85, 3f, 12f, 3f, 3f),
        FoodEntry(listOf("aloo matar", "potato peas"), "1 katori", 150, 95, 3.5f, 13f, 3.5f, 3f),
        FoodEntry(listOf("bhindi masala", "okra masala"), "1 katori", 150, 78, 3f, 10f, 3f, 3.5f),
        FoodEntry(listOf("baingan bharta", "brinjal", "eggplant"), "1 katori", 150, 70, 2.5f, 10f, 2.5f, 3f),
        FoodEntry(listOf("palak", "spinach", "saag"), "1 katori", 100, 50, 4f, 7f, 0.8f, 3f),
        FoodEntry(listOf("mixed veg", "vegetable curry", "sabzi", "sabji"), "1 katori", 150, 82, 3.2f, 10f, 3.5f, 3f),

        // ── Chicken & Meat ─────────────────────────────────────────────────
        FoodEntry(listOf("murgh makhani", "butter chicken"), "1 portion", 200, 188, 16f, 8f, 11f, 0.5f),
        FoodEntry(listOf("chicken keema", "keema", "mince chicken", "chicken mince"), "1 katori", 150, 175, 19f, 5f, 9f, 1f),
        FoodEntry(listOf("chicken curry", "chicken gravy", "chicken masala"), "1 portion", 200, 166, 16f, 5f, 9f, 0.5f),
        FoodEntry(listOf("chicken tikka", "tandoori chicken", "grilled chicken"), "2 pieces", 150, 150, 22f, 2f, 6f, 0f),
        FoodEntry(listOf("chicken breast", "boiled chicken", "steamed chicken"), "1 piece", 120, 165, 31f, 0f, 3.6f, 0f),
        FoodEntry(listOf("mutton curry", "lamb curry", "gosht"), "1 portion", 200, 213, 18f, 5f, 14f, 0.5f),
        FoodEntry(listOf("prawn curry", "shrimp curry", "prawn masala"), "1 portion", 150, 120, 18f, 4f, 4f, 0.5f),
        FoodEntry(listOf("fish curry", "fish gravy", "fish masala"), "1 portion", 200, 134, 18f, 4f, 5f, 0.5f),
        FoodEntry(listOf("fish", "rohu", "pomfret", "salmon", "catla", "tuna", "tilapia"), "1 piece", 120, 120, 20f, 0f, 4f, 0f),
        FoodEntry(listOf("egg curry", "anda curry", "egg masala"), "1 portion", 150, 168, 11f, 5f, 12f, 0.5f),
        FoodEntry(listOf("egg bhurji", "anda bhurji", "scrambled egg", "bhurji"), "1 portion", 100, 165, 13f, 2f, 12f, 0f),
        FoodEntry(listOf("omelette", "omelet", "egg omelette"), "1 omelette", 80, 155, 11f, 1f, 11f, 0f),
        FoodEntry(listOf("boiled egg", "egg boil", "hard boiled egg"), "1 egg", 50, 78, 6.3f, 0.6f, 5.3f, 0f),
        FoodEntry(listOf("egg", "eggs", "anda"), "1 egg", 50, 155, 13f, 1.1f, 11f, 0f),

        // ── Street Food & Snacks ───────────────────────────────────────────
        FoodEntry(listOf("chole bhature", "choley bhature"), "1 plate", 350, 234, 7.8f, 34f, 8f, 3f),
        FoodEntry(listOf("misal pav"), "1 plate", 250, 280, 10f, 40f, 8f, 6f),
        FoodEntry(listOf("vada pav", "wada pav", "vadapav"), "1 piece", 130, 270, 6f, 38f, 11f, 2f),
        FoodEntry(listOf("pani puri", "golgappa", "puchka", "panipuri"), "6 pieces", 120, 190, 4f, 33f, 5f, 2f),
        FoodEntry(listOf("bhel puri", "bhelpuri"), "1 plate", 150, 180, 4f, 33f, 4f, 3f),
        FoodEntry(listOf("sev puri"), "6 pieces", 100, 200, 4.5f, 30f, 7f, 2.5f),
        FoodEntry(listOf("dahi puri"), "6 pieces", 130, 190, 5f, 30f, 6f, 2f),
        FoodEntry(listOf("aloo tikki", "alu tikki"), "2 pieces", 120, 220, 4f, 35f, 8f, 3f),
        FoodEntry(listOf("bread pakora", "bread bhajiya"), "1 piece", 100, 285, 7f, 34f, 14f, 2f),
        FoodEntry(listOf("pakora", "pakoda", "bhajiya"), "4 pieces", 80, 285, 6.5f, 30f, 16f, 2f),
        FoodEntry(listOf("samosa"), "1 samosa", 80, 308, 5.7f, 36f, 16f, 2f),
        FoodEntry(listOf("kachori", "matar kachori"), "1 kachori", 80, 350, 6f, 40f, 18f, 3f),
        FoodEntry(listOf("frankie", "kathi roll", "kati roll"), "1 roll", 180, 280, 9f, 38f, 10f, 2f),
        FoodEntry(listOf("dabeli"), "1 dabeli", 120, 260, 6f, 36f, 10f, 3f),
        FoodEntry(listOf("dhokla"), "4 pieces", 100, 160, 7f, 25f, 4f, 2f),
        FoodEntry(listOf("khandvi"), "4 rolls", 80, 175, 6f, 22f, 6.5f, 2f),

        // ── Common Fast Food ───────────────────────────────────────────────
        FoodEntry(listOf("veg pizza"), "1 slice", 100, 250, 10f, 32f, 8f, 2f),
        FoodEntry(listOf("cheese pizza", "pizza"), "1 slice", 100, 266, 11f, 33f, 10f, 2.3f),
        FoodEntry(listOf("chicken burger"), "1 burger", 180, 350, 20f, 36f, 14f, 2f),
        FoodEntry(listOf("veg burger", "burger", "hamburger"), "1 burger", 150, 295, 13f, 35f, 12f, 2f),
        FoodEntry(listOf("club sandwich"), "1 sandwich", 150, 280, 12f, 36f, 10f, 3f),
        FoodEntry(listOf("sandwich", "veg sandwich"), "1 sandwich", 120, 242, 9f, 35f, 7f, 2.5f),
        FoodEntry(listOf("french fries", "chips", "fries"), "1 portion", 100, 312, 3.4f, 41f, 15f, 3.8f),
        FoodEntry(listOf("maggi", "instant noodles", "2 minute noodles"), "1 pack", 70, 332, 8.5f, 49f, 12f, 2f),
        FoodEntry(listOf("hakka noodles", "chow mein", "noodles"), "1 plate", 180, 220, 6f, 38f, 5f, 2f),
        FoodEntry(listOf("pasta", "spaghetti", "penne"), "1 plate", 180, 220, 8f, 43f, 1.5f, 2.5f),

        // ── Breakfast ─────────────────────────────────────────────────────
        FoodEntry(listOf("kanda poha", "aloo poha"), "1 plate", 200, 120, 3f, 24f, 2f, 1.5f),
        FoodEntry(listOf("poha", "pohe", "flattened rice"), "1 plate", 200, 113, 2.5f, 23f, 1.8f, 1f),
        FoodEntry(listOf("vermicelli upma", "semiya upma", "sevai upma"), "1 plate", 200, 200, 6f, 36f, 4f, 2f),
        FoodEntry(listOf("rava upma", "sooji upma", "semolina upma", "upma"), "1 plate", 200, 110, 3.2f, 20f, 2.5f, 1.5f),
        FoodEntry(listOf("oats porridge", "rolled oats", "overnight oats", "oatmeal", "oats"), "1 bowl", 250, 71, 2.5f, 12f, 1.5f, 1.7f),
        FoodEntry(listOf("corn flakes", "cornflakes"), "1 bowl with milk", 200, 150, 5f, 29f, 2f, 1.5f),
        FoodEntry(listOf("muesli", "granola"), "1 bowl", 80, 370, 9f, 64f, 7f, 5f),
        FoodEntry(listOf("paniyaram", "paddu", "appe"), "4 pieces", 80, 135, 4f, 22f, 3.5f, 1.5f),

        // ── Fruits ────────────────────────────────────────────────────────
        FoodEntry(listOf("papaya", "papita"), "1 cup", 140, 43, 0.5f, 11f, 0.3f, 1.7f),
        FoodEntry(listOf("watermelon", "tarbuz"), "2 slices", 300, 30, 0.6f, 7.6f, 0.2f, 0.4f),
        FoodEntry(listOf("pomegranate", "anar"), "1 cup", 100, 83, 1.7f, 19f, 1.2f, 4f),
        FoodEntry(listOf("pineapple", "ananas"), "2 slices", 150, 50, 0.5f, 13f, 0.1f, 1.4f),
        FoodEntry(listOf("chikoo", "sapota", "chickoo"), "1 fruit", 100, 83, 0.4f, 20f, 1.1f, 5.3f),
        FoodEntry(listOf("guava", "amrood"), "1 guava", 100, 68, 2.6f, 14f, 1f, 5.4f),
        FoodEntry(listOf("grapes", "angoor"), "1 cup", 150, 67, 0.6f, 17f, 0.4f, 0.9f),
        FoodEntry(listOf("strawberry", "strawberries"), "1 cup", 150, 33, 0.7f, 7.7f, 0.3f, 2f),
        FoodEntry(listOf("mango", "aam", "alphonso"), "1 medium", 200, 60, 0.8f, 15f, 0.4f, 1.6f),
        FoodEntry(listOf("orange", "santra", "narangi"), "1 medium", 130, 47, 0.9f, 12f, 0.1f, 2.4f),
        FoodEntry(listOf("banana", "kela"), "1 medium", 120, 89, 1.1f, 23f, 0.3f, 2.6f),
        FoodEntry(listOf("apple", "seb"), "1 medium", 150, 52, 0.3f, 14f, 0.2f, 2.4f),
        FoodEntry(listOf("lemon", "nimbu"), "1 lemon", 58, 29, 1.1f, 9.3f, 0.3f, 2.8f),

        // ── Dairy & Protein ────────────────────────────────────────────────
        FoodEntry(listOf("skim milk", "skimmed milk", "double toned milk"), "1 glass", 240, 34, 3.4f, 5f, 0.1f, 0f),
        FoodEntry(listOf("milk", "doodh", "whole milk", "toned milk", "full cream milk"), "1 glass", 240, 61, 3.2f, 4.8f, 3.3f, 0f),
        FoodEntry(listOf("greek yogurt", "hung curd"), "1 katori", 150, 100, 10f, 4f, 4f, 0f),
        FoodEntry(listOf("curd", "dahi", "yogurt", "plain curd", "yoghurt"), "1 katori", 150, 60, 3.5f, 4.7f, 3.1f, 0f),
        FoodEntry(listOf("sweet lassi"), "1 glass", 250, 80, 3.5f, 13f, 2f, 0f),
        FoodEntry(listOf("lassi"), "1 glass", 250, 62, 3f, 9f, 1.5f, 0f),
        FoodEntry(listOf("chaas", "buttermilk", "chaach"), "1 glass", 250, 37, 2.8f, 5f, 0.6f, 0f),
        FoodEntry(listOf("paneer raw", "fresh paneer", "cottage cheese"), "100g", 100, 265, 18f, 3.4f, 20f, 0f),
        FoodEntry(listOf("ghee"), "1 tsp", 5, 900, 0f, 0f, 100f, 0f),
        FoodEntry(listOf("butter"), "1 tsp", 5, 717, 0.9f, 0.1f, 81f, 0f),
        FoodEntry(listOf("whey protein", "protein powder", "protein shake"), "1 scoop", 30, 370, 75f, 10f, 5f, 0f),

        // ── Beverages ──────────────────────────────────────────────────────
        FoodEntry(listOf("green tea", "herbal tea"), "1 cup", 240, 2, 0f, 0.5f, 0f, 0f),
        FoodEntry(listOf("turmeric milk", "haldi doodh", "golden milk"), "1 glass", 240, 65, 3.2f, 6f, 3.3f, 0f),
        FoodEntry(listOf("coconut water", "nariyal pani"), "1 glass", 240, 19, 0.7f, 3.7f, 0.2f, 1f),
        FoodEntry(listOf("nimbu pani", "lemonade", "shikanji", "lemon water"), "1 glass", 250, 20, 0.3f, 5f, 0f, 0.2f),
        FoodEntry(listOf("sugarcane juice", "ganna juice"), "1 glass", 250, 70, 0.5f, 17f, 0f, 0.6f),
        FoodEntry(listOf("orange juice", "mosambi juice", "sweet lime juice"), "1 glass", 240, 45, 0.7f, 10f, 0.2f, 0.5f),
        FoodEntry(listOf("mango shake", "mango lassi", "aam panna"), "1 glass", 250, 80, 1f, 18f, 1f, 0.5f),
        FoodEntry(listOf("coffee with milk", "milk coffee", "latte", "cappuccino"), "1 cup", 240, 40, 1.8f, 4.8f, 1.5f, 0f),
        FoodEntry(listOf("black coffee", "filter coffee", "instant coffee", "coffee"), "1 cup", 240, 2, 0.3f, 0f, 0f, 0f),
        FoodEntry(listOf("masala chai", "masala tea", "milk tea", "chai", "tea"), "1 cup", 180, 28, 1.2f, 4f, 0.8f, 0f),

        // ── Nuts & Seeds ───────────────────────────────────────────────────
        FoodEntry(listOf("pistachios", "pistachio", "pista"), "20 kernels", 14, 562, 20f, 28f, 45f, 10.6f),
        FoodEntry(listOf("walnuts", "walnut", "akhrot"), "4 halves", 14, 654, 15f, 14f, 65f, 6.7f),
        FoodEntry(listOf("cashews", "cashew", "kaju"), "10 cashews", 14, 553, 18f, 30f, 44f, 3.3f),
        FoodEntry(listOf("almonds", "almond", "badam"), "10 almonds", 14, 579, 21f, 22f, 50f, 12.5f),
        FoodEntry(listOf("peanuts", "groundnuts", "moongfali"), "1 handful", 30, 567, 26f, 16f, 49f, 8.5f),
        FoodEntry(listOf("chia seeds", "chia"), "1 tbsp", 12, 486, 17f, 42f, 31f, 34f),
        FoodEntry(listOf("flaxseeds", "flax seeds", "alsi"), "1 tbsp", 10, 534, 18f, 29f, 42f, 27f),
        FoodEntry(listOf("sunflower seeds"), "1 tbsp", 10, 584, 21f, 20f, 51f, 8.6f),

        // ── Plant Protein ─────────────────────────────────────────────────
        FoodEntry(listOf("soya chunks", "soy chunks", "meal maker"), "1 katori cooked", 100, 150, 17f, 15f, 3f, 4f),
        FoodEntry(listOf("tofu", "soy paneer"), "100g", 100, 76, 8f, 1.9f, 4.8f, 0.3f),
        FoodEntry(listOf("moong sprouts", "mixed sprouts", "sprouts", "bean sprouts"), "1 katori", 100, 97, 7f, 17f, 0.6f, 4f),

        // ── Sweets & Desserts ──────────────────────────────────────────────
        FoodEntry(listOf("gulab jamun"), "2 pieces", 80, 368, 5f, 54f, 15f, 0.5f),
        FoodEntry(listOf("jalebi"), "2 pieces", 60, 375, 3f, 63f, 13f, 0.5f),
        FoodEntry(listOf("rasmalai"), "2 pieces", 100, 170, 7f, 22f, 7f, 0.2f),
        FoodEntry(listOf("rasgulla"), "2 pieces", 100, 186, 5.8f, 35f, 3.3f, 0.3f),
        FoodEntry(listOf("gajar halwa", "carrot halwa"), "1 katori", 100, 260, 4f, 38f, 11f, 2f),
        FoodEntry(listOf("sooji halwa", "halwa"), "1 katori", 100, 280, 4f, 42f, 11f, 1f),
        FoodEntry(listOf("kaju katli"), "2 pieces", 40, 450, 10f, 55f, 22f, 1f),
        FoodEntry(listOf("besan ladoo", "bundi ladoo", "motichoor ladoo", "ladoo"), "1 piece", 50, 450, 6f, 62f, 20f, 1f),
        FoodEntry(listOf("barfi", "burfi", "milk cake"), "1 piece", 40, 390, 8f, 55f, 15f, 0.5f),
        FoodEntry(listOf("modak"), "2 pieces", 80, 280, 4f, 42f, 11f, 2f),
        FoodEntry(listOf("shrikhand"), "1 katori", 100, 195, 8f, 28f, 6f, 0f),
        FoodEntry(listOf("payasam", "kheer", "rice pudding"), "1 katori", 150, 130, 4f, 22f, 4f, 0.2f),
        FoodEntry(listOf("kulfi"), "1 stick", 80, 195, 4f, 24f, 9f, 0f),
        FoodEntry(listOf("ice cream"), "1 scoop", 80, 200, 3.5f, 24f, 10f, 0f),

        // ── General ───────────────────────────────────────────────────────
        FoodEntry(listOf("tomato soup", "vegetable soup", "mushroom soup", "soup"), "1 bowl", 250, 50, 2f, 8f, 1f, 2f),
        FoodEntry(listOf("green salad", "mixed salad", "salad"), "1 plate", 150, 35, 2f, 7f, 0.5f, 3f),
    )

    // Sort by longest alias first — "moong dal" beats "dal", prevents double-counting
    private val SORTED_DB = DATABASE.sortedByDescending { e -> e.names.maxOf { it.length } }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    fun parse(voiceText: String): Result<FoodNutrition> {
        val text  = voiceText.lowercase().trim()
        val items = extractItems(text)

        if (items.isEmpty()) {
            return Result.failure(
                Exception("Food not recognised. Try: \"2 rotis and dal\", \"a bowl of curd rice\", \"200g chicken breast\"")
            )
        }

        var totalCalories = 0
        var totalProtein  = 0f
        var totalCarbs    = 0f
        var totalFat      = 0f
        var totalFiber    = 0f
        val names         = mutableListOf<String>()
        val servings      = mutableListOf<String>()

        for ((entry, grams) in items) {
            totalCalories += (entry.caloriesPer100g * grams / 100f).roundToInt()
            totalProtein  += entry.proteinPer100g  * grams / 100f
            totalCarbs    += entry.carbsPer100g    * grams / 100f
            totalFat      += entry.fatPer100g      * grams / 100f
            totalFiber    += entry.fiberPer100g    * grams / 100f
            val name = entry.names.first()
            names.add(name.replaceFirstChar { it.uppercase() })
            servings.add(
                if (grams == entry.servingGrams.toFloat()) "${entry.servingLabel} $name"
                else "${grams.roundToInt()}g $name"
            )
        }

        return Result.success(
            FoodNutrition(
                foodName    = names.joinToString(" + "),
                servingSize = servings.joinToString(", "),
                calories    = totalCalories,
                proteinG    = totalProtein,
                carbsG      = totalCarbs,
                fatG        = totalFat,
                fiberG      = totalFiber,
                confidence  = if (items.size == 1) "high" else "medium",
                notes       = "Local database · offline"
            )
        )
    }

    fun findFood(name: String): FoodEntry? {
        val lower = name.lowercase()
        return SORTED_DB.firstOrNull { e -> e.names.any { lower.contains(it) } }
    }

    val allFoodNames: List<String> get() = DATABASE.flatMap { it.names }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — parsing logic
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns (entry, grams) pairs.
     * Replaces matched regions in text so "moong dal" doesn't also match "dal".
     */
    private fun extractItems(text: String): List<Pair<FoodEntry, Float>> {
        val results = mutableListOf<Pair<FoodEntry, Float>>()
        val buf     = StringBuilder(text)

        for (entry in SORTED_DB) {
            for (alias in entry.names.sortedByDescending { it.length }) {
                val idx = buf.indexOf(alias)
                if (idx >= 0) {
                    val grams = extractGrams(buf.toString(), idx, entry.servingGrams)
                    results.add(entry to grams)
                    // Erase matched text so shorter aliases can't re-match it
                    for (i in idx until idx + alias.length) buf.setCharAt(i, ' ')
                    break
                }
            }
        }
        return results
    }

    /** Determine actual gram weight from text preceding the food match. */
    private fun extractGrams(text: String, foodIdx: Int, defaultGrams: Int): Float {
        if (foodIdx <= 0) return defaultGrams.toFloat()
        val before = text.substring(0, foodIdx).trim()

        // "200g", "200gm", "200 grams"
        val gramsMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:g|grams?|gm)\\s*$").find(before)
        if (gramsMatch != null) return gramsMatch.groupValues[1].toFloat()

        // Digit: "2 rotis" → 2 × defaultGrams
        val digitMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*$").find(before)
        if (digitMatch != null) return digitMatch.groupValues[1].toFloat() * defaultGrams

        // Word: "two rotis", "a bowl of dal"
        val words = before.split(Regex("\\s+"))
        for (word in words.reversed()) {
            val n = WORD_TO_NUM[word] ?: continue
            return (n * defaultGrams).toFloat()
        }
        return defaultGrams.toFloat()
    }
}
