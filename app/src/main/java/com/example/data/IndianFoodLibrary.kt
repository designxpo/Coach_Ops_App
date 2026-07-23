package com.example.data

/**
 * A broad, browsable library of common Indian foods (veg + non-veg, North &
 * South, regional) with approximate per-serving nutrition. This is reference
 * content the member can explore/search/filter — separate from the goal-based
 * meal PLANS in [NutritionRepository]. Values are typical home-portion estimates.
 */

// ─── Regional cuisine ─────────────────────────────────────────────────────────

enum class IndianRegion(val label: String) {
    NORTH("North Indian"),
    SOUTH("South Indian"),
    WEST("West Indian"),
    EAST("East Indian")
}

/**
 * Best-effort map from the member's saved city/area text to a culinary region,
 * so region-relevant dishes can be surfaced first. Returns null when unknown
 * (then the default order is kept).
 */
fun regionForLocation(location: String?): IndianRegion? {
    val s = (location ?: "").lowercase()
    if (s.isBlank()) return null
    fun any(vararg keys: String) = keys.any { it in s }
    return when {
        any("chennai", "madras", "tamil", "bengaluru", "bangalore", "karnataka", "kerala",
            "kochi", "cochin", "trivandrum", "thiruvananthapuram", "hyderabad", "telangana",
            "andhra", "vijayawada", "visakhapatnam", "vizag", "mysore", "mysuru", "coimbatore",
            "madurai", "mangalore", "udupi", "salem", "tirupati", "guntur") -> IndianRegion.SOUTH
        any("kolkata", "calcutta", "bengal", "assam", "guwahati", "odisha", "orissa",
            "bhubaneswar", "patna", "bihar", "ranchi", "jharkhand", "siliguri", "durgapur",
            "cuttack", "shillong", "agartala") -> IndianRegion.EAST
        any("mumbai", "bombay", "pune", "maharashtra", "nagpur", "nashik", "gujarat",
            "ahmedabad", "surat", "vadodara", "rajkot", "goa", "panaji", "thane", "aurangabad",
            "kolhapur", "indore", "bhopal", "madhya") -> IndianRegion.WEST
        any("delhi", "punjab", "amritsar", "ludhiana", "chandigarh", "haryana", "gurgaon",
            "gurugram", "noida", "lucknow", "kanpur", "uttar pradesh", "jaipur", "rajasthan",
            "jammu", "kashmir", "srinagar", "himachal", "shimla", "uttarakhand", "dehradun",
            "agra", "varanasi", "jalandhar", "patiala", "meerut", "faridabad") -> IndianRegion.NORTH
        else -> null
    }
}

/** The signature region of a dish, or null if it's pan-Indian (shown everywhere). */
fun regionOfDish(name: String): IndianRegion? {
    val n = name.lowercase()
    fun any(vararg keys: String) = keys.any { it in n }
    return when {
        any("dosa", "idli", "sambar", "uttapam", "ragi", "medu vada", "curd rice",
            "lemon rice", "coconut water", "rasam") -> IndianRegion.SOUTH
        any("poha", "dhokla", "thepla", "sabudana", "pav bhaji", "bhel", "shrikhand",
            "vada pav", "misal") -> IndianRegion.WEST
        any("rasgulla", "sandesh", "sattu", "kheer") -> IndianRegion.EAST
        any("paratha", "chole", "chana masala", "rajma", "dal makhani", "paneer butter",
            "malai kofta", "naan", "tandoori", "missi", "lassi", "gajar halwa", "biryani",
            "keema") -> IndianRegion.NORTH
        else -> null
    }
}

enum class IndianFoodCategory(val label: String, val image: String) {
    BREAKFAST("Breakfast", u("1614961233913-a5113a4a34ed")),
    MAINS("Sabzi & Mains", u("1585937421612-70a008356fbe")),
    DAL_CURRY("Dal & Curry", u("1546833999-b9f581a1996d")),
    BREADS_RICE("Breads & Rice", u("1516684732162-798a0062be99")),
    SNACKS("Snacks & Chaat", u("1601050690597-df0568f70950")),
    SALADS_SIDES("Salads & Sides", u("1512621776951-a57141f2eefd")),
    DRINKS("Drinks", u("1556742049-0cfed4f6a45d")),
    NONVEG("Non-Veg", u("1567337710282-00832b415979")),
    SWEETS("Sweets", u("1666190092159-3171cf0fbb12"))
}

private fun u(id: String) = "https://images.unsplash.com/photo-$id?w=200&h=200&fit=crop&q=80"

data class LibraryFood(
    val name: String,
    val quantity: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val isVegetarian: Boolean,
    val category: IndianFoodCategory,
    val benefits: String
)

object IndianFoodLibrary {

    private fun f(
        name: String, qty: String, cal: Int, pro: Int, carb: Int, fat: Int,
        cat: IndianFoodCategory, benefit: String, veg: Boolean = true
    ) = LibraryFood(name, qty, cal, pro, carb, fat, veg, cat, benefit)

    private val B = IndianFoodCategory.BREAKFAST
    private val M = IndianFoodCategory.MAINS
    private val D = IndianFoodCategory.DAL_CURRY
    private val R = IndianFoodCategory.BREADS_RICE
    private val S = IndianFoodCategory.SNACKS
    private val L = IndianFoodCategory.SALADS_SIDES
    private val W = IndianFoodCategory.DRINKS
    private val N = IndianFoodCategory.NONVEG
    private val T = IndianFoodCategory.SWEETS

    val all: List<LibraryFood> = listOf(
        // ── Breakfast ─────────────────────────────────────────────────────────
        f("Masala Dosa", "2 dosa + chutney", 380, 8, 60, 12, B, "Fermented rice-lentil batter — gut-friendly, light energy"),
        f("Idli & Sambar", "3 idli + sambar", 250, 10, 46, 3, B, "Steamed, low-fat, easy to digest; sambar adds protein & fibre"),
        f("Plain Dosa", "2 dosa", 260, 6, 46, 6, B, "Light fermented carbs, low fat"),
        f("Rava Idli", "3 idli", 230, 7, 40, 4, B, "Semolina-based, quick energy"),
        f("Poha", "1 bowl", 270, 6, 48, 6, B, "Light, iron-rich flattened rice with peanuts"),
        f("Upma", "1 bowl", 250, 7, 42, 6, B, "Semolina + veggies — filling, low fat"),
        f("Aloo Paratha", "2 paratha + curd", 420, 11, 60, 16, B, "Comforting carbs; pair with curd for protein"),
        f("Paneer Paratha", "2 paratha", 420, 18, 54, 14, B, "High protein + complex carbs for muscle fuel"),
        f("Methi Thepla", "3 thepla", 300, 8, 44, 10, B, "Fenugreek adds fibre; travel-friendly"),
        f("Besan Chilla", "2 chilla", 240, 12, 26, 9, B, "Gram-flour pancake — high plant protein"),
        f("Moong Dal Chilla", "2 chilla", 220, 14, 24, 6, B, "Very high protein, low fat, diabetic-friendly"),
        f("Vegetable Uttapam", "2 pieces", 300, 8, 50, 8, B, "Thick fermented pancake loaded with veggies"),
        f("Sabudana Khichdi", "1 bowl", 320, 5, 52, 11, B, "Fasting-friendly quick energy from tapioca"),
        f("Vegetable Dalia", "1 bowl", 220, 8, 40, 4, B, "Broken-wheat porridge — high fibre, filling"),
        f("Oats Upma", "1 bowl", 240, 9, 38, 6, B, "Beta-glucan oats + veggies for heart health"),
        f("Ragi Dosa", "2 dosa", 250, 8, 44, 5, B, "Finger-millet — rich in calcium & iron"),
        f("Masala Oats", "1 bowl", 230, 9, 36, 6, B, "Savoury oats — steady energy, cholesterol-friendly"),
        f("Egg Bhurji", "2 eggs + pav", 320, 16, 26, 16, B, "Complete protein scramble", veg = false),
        f("Boiled Eggs", "2 eggs", 155, 13, 1, 11, B, "Cheap, complete protein; great post-workout", veg = false),

        // ── Sabzi & Mains ───────────────────────────────────────────────────────
        f("Palak Paneer", "1 cup", 300, 15, 12, 22, M, "Iron-rich spinach + high-protein paneer"),
        f("Paneer Butter Masala", "1 cup", 350, 14, 14, 26, M, "Rich, high-protein treat; watch the portion"),
        f("Matar Paneer", "1 cup", 300, 14, 16, 20, M, "Protein from paneer + fibre from peas"),
        f("Bhindi Masala", "1 cup", 160, 4, 14, 10, M, "Okra — fibre-rich, blood-sugar friendly"),
        f("Baingan Bharta", "1 cup", 150, 4, 14, 9, M, "Smoky roasted aubergine, low calorie"),
        f("Aloo Gobi", "1 cup", 180, 5, 24, 8, M, "Cauliflower + potato — comforting, veggie-rich"),
        f("Mixed Veg Sabzi", "1 cup", 150, 5, 18, 7, M, "Assorted vegetables — vitamins & fibre"),
        f("Chana Masala", "1 cup", 260, 12, 34, 8, M, "Chickpeas — high plant protein & fibre"),
        f("Rajma Masala", "1 cup", 250, 13, 36, 6, M, "Kidney beans — protein, iron & fibre"),
        f("Soya Chunk Curry", "1 cup", 230, 22, 18, 8, M, "Very high protein — excellent for vegetarians"),
        f("Malai Kofta", "1 cup", 380, 10, 22, 28, M, "Indulgent; balance with a light side"),
        f("Dum Aloo", "1 cup", 240, 5, 30, 12, M, "Baby potatoes in spiced gravy"),
        f("Mushroom Masala", "1 cup", 190, 8, 14, 12, M, "Low-cal, umami, B-vitamins"),
        f("Bhindi Do Pyaza", "1 cup", 170, 4, 15, 10, M, "Okra with onions — fibre & flavour"),

        // ── Dal & Curry ─────────────────────────────────────────────────────────
        f("Dal Tadka", "1 cup", 180, 12, 24, 4, D, "Everyday protein-rich lentils"),
        f("Dal Makhani", "1 cup", 280, 12, 26, 14, D, "Creamy black dal — hearty, protein-rich"),
        f("Moong Dal", "1 cup", 150, 12, 22, 2, D, "Lightest dal — easy to digest, low fat"),
        f("Toor Dal", "1 cup", 170, 11, 24, 3, D, "Staple pigeon-pea dal, high protein"),
        f("Sambar", "1 cup", 140, 8, 20, 4, D, "Lentil-veg stew — protein + veggies"),
        f("Kadhi", "1 cup", 190, 8, 16, 10, D, "Yogurt-gram flour curry — probiotic"),
        f("Chana Dal", "1 cup", 190, 12, 28, 4, D, "Split chickpeas — fibre & slow carbs"),
        f("Lobia (Black-eyed Peas)", "1 cup", 200, 13, 30, 3, D, "High protein & folate"),
        f("Mixed Dal", "1 cup", 180, 12, 26, 3, D, "Blend of lentils — complete amino profile"),
        f("Palak Dal", "1 cup", 170, 12, 22, 4, D, "Lentils + spinach — protein & iron"),

        // ── Breads & Rice ───────────────────────────────────────────────────────
        f("Roti / Chapati", "2 roti", 200, 6, 40, 2, R, "Whole-wheat staple — steady carbs & fibre"),
        f("Tandoori Roti", "2 roti", 240, 7, 46, 3, R, "Clay-oven whole wheat bread"),
        f("Jowar Bhakri", "2 bhakri", 220, 6, 44, 2, R, "Sorghum millet — gluten-free, high fibre"),
        f("Bajra Roti", "2 roti", 240, 7, 44, 4, R, "Pearl millet — iron & magnesium rich"),
        f("Missi Roti", "2 roti", 260, 9, 42, 6, R, "Gram + wheat flour — extra protein"),
        f("Plain Naan", "1 naan", 260, 8, 48, 5, R, "Refined-flour treat; keep it occasional"),
        f("Steamed Rice", "1 cup", 200, 4, 45, 0, R, "Simple energy; pair with dal for protein"),
        f("Jeera Rice", "1 cup", 240, 4, 45, 5, R, "Cumin-flavoured rice"),
        f("Brown Rice", "1 cup", 220, 5, 46, 2, R, "Whole grain — more fibre than white rice"),
        f("Veg Pulao", "1 cup", 280, 6, 46, 8, R, "One-pot rice with veggies"),
        f("Curd Rice", "1 cup", 250, 8, 40, 6, R, "Cooling probiotic comfort food"),
        f("Lemon Rice", "1 cup", 260, 5, 44, 7, R, "Tangy South-Indian rice with peanuts"),
        f("Khichdi", "1 bowl", 260, 11, 42, 5, R, "Rice + dal — complete, easy-to-digest meal"),
        f("Vegetable Biryani", "1 plate", 380, 9, 58, 12, R, "Fragrant spiced rice with veggies"),
        f("Rajma Chawal", "1 plate", 400, 15, 66, 8, R, "Classic protein + carb combo"),

        // ── Snacks & Chaat ────────────────────────────────────────────────────────
        f("Dhokla", "4 pieces", 180, 7, 30, 3, S, "Steamed, fermented gram flour — light & low fat"),
        f("Sprout Chaat", "1 bowl", 150, 10, 22, 2, S, "Sprouted moong — high protein, live enzymes"),
        f("Roasted Chana", "40 g", 160, 9, 22, 3, S, "Crunchy high-protein, high-fibre snack"),
        f("Bhel Puri", "1 plate", 250, 6, 42, 7, S, "Puffed rice chaat — light but tasty"),
        f("Vegetable Cutlet", "2 pieces", 220, 5, 28, 10, S, "Veg-packed pan-fried snack"),
        f("Paneer Tikka", "6 pieces", 280, 20, 8, 18, S, "Grilled paneer — high protein starter"),
        f("Samosa", "1 piece", 260, 4, 30, 14, S, "Deep-fried treat; enjoy occasionally"),
        f("Pav Bhaji", "1 plate", 400, 9, 54, 16, S, "Buttery veg mash with pav — indulgent"),
        f("Fruit Chaat", "1 bowl", 130, 2, 30, 1, S, "Mixed fruit — vitamins & natural sugars"),
        f("Makhana (Roasted)", "30 g", 120, 4, 20, 2, S, "Fox nuts — low-cal, calcium & magnesium"),
        f("Peanut Chikki", "1 piece", 180, 6, 18, 10, S, "Jaggery-peanut energy bite"),
        f("Masala Corn", "1 cup", 160, 5, 30, 3, S, "Sweetcorn with spices — fibre & fun"),
        f("Vegetable Sandwich", "2 slices", 240, 8, 34, 8, S, "Quick veg-loaded snack"),

        // ── Salads & Sides ──────────────────────────────────────────────────────
        f("Kachumber Salad", "1 bowl", 60, 2, 10, 1, L, "Cucumber-onion-tomato — hydrating & fresh"),
        f("Sprouts Salad", "1 bowl", 140, 9, 20, 2, L, "High-protein raw sprouts"),
        f("Cucumber Raita", "1 cup", 90, 5, 8, 4, L, "Cooling probiotic yogurt side"),
        f("Boondi Raita", "1 cup", 140, 5, 12, 8, L, "Yogurt with crisp gram-flour pearls"),
        f("Green Salad", "1 plate", 50, 2, 8, 1, L, "Raw veggies — fibre, near-zero calories"),
        f("Beetroot Salad", "1 bowl", 90, 3, 16, 2, L, "Nitrates support blood flow & stamina"),
        f("Mixed Veg Raita", "1 cup", 110, 5, 10, 5, L, "Yogurt + veggies — probiotic & filling"),

        // ── Drinks ──────────────────────────────────────────────────────────────
        f("Masala Chai", "1 cup", 90, 2, 12, 4, W, "Spiced milk tea — comforting pick-me-up"),
        f("Buttermilk (Chaas)", "1 glass", 60, 3, 5, 3, W, "Probiotic, cooling, aids digestion"),
        f("Sweet Lassi", "1 glass", 180, 6, 28, 5, W, "Yogurt drink — protein + quick energy"),
        f("Coconut Water", "1 glass", 45, 2, 9, 0, W, "Natural electrolytes for hydration"),
        f("Nimbu Pani", "1 glass", 70, 0, 18, 0, W, "Lemon water — vitamin C, refreshing"),
        f("Green Tea", "1 cup", 5, 0, 1, 0, W, "Antioxidants, near-zero calories"),
        f("Badam Milk", "1 glass", 180, 8, 20, 8, W, "Almond-milk — protein, calcium, healthy fats"),
        f("Sattu Drink", "1 glass", 190, 11, 28, 4, W, "Roasted-gram energy drink — high protein"),

        // ── Non-Veg ───────────────────────────────────────────────────────────────
        f("Chicken Curry", "1 cup", 300, 28, 8, 16, N, "Lean protein in spiced gravy", veg = false),
        f("Tandoori Chicken", "2 pieces", 260, 32, 4, 12, N, "Grilled — very high protein, lower fat", veg = false),
        f("Grilled Chicken Breast", "150 g", 250, 40, 0, 9, N, "Leanest high-protein option", veg = false),
        f("Chicken Biryani", "1 plate", 480, 26, 58, 16, N, "Complete meal — protein + carbs", veg = false),
        f("Egg Curry", "2 eggs", 280, 16, 10, 18, N, "Protein-rich eggs in gravy", veg = false),
        f("Fish Curry", "1 cup", 250, 26, 8, 12, N, "Omega-3 rich lean protein", veg = false),
        f("Grilled Fish", "150 g", 220, 30, 0, 10, N, "Omega-3s for heart & recovery", veg = false),
        f("Chicken Tikka", "6 pieces", 250, 30, 6, 12, N, "Grilled high-protein starter", veg = false),
        f("Chicken Keema", "1 cup", 320, 26, 8, 20, N, "Minced chicken — protein-dense", veg = false),
        f("Mutton Curry", "1 cup", 380, 26, 6, 28, N, "Iron & B12 rich; higher in fat", veg = false),
        f("Fish Fry", "2 pieces", 300, 24, 8, 18, N, "Crisp protein; pair with salad", veg = false),
        f("Egg Omelette", "2 eggs", 220, 14, 3, 16, N, "Quick complete-protein meal", veg = false),

        // ── Sweets ────────────────────────────────────────────────────────────────
        f("Gulab Jamun", "1 piece", 150, 2, 22, 6, T, "Festive treat — enjoy in moderation"),
        f("Rasgulla", "1 piece", 110, 3, 22, 1, T, "Lighter chhena sweet, lower fat"),
        f("Kheer", "1 bowl", 250, 6, 40, 8, T, "Rice-milk pudding — calcium & comfort"),
        f("Gajar Halwa", "1 bowl", 300, 5, 40, 14, T, "Carrot dessert — vitamin A; rich in ghee"),
        f("Besan Ladoo", "1 piece", 180, 4, 20, 10, T, "Gram-flour sweet — some protein"),
        f("Fruit Custard", "1 bowl", 200, 5, 34, 5, T, "Fruit + milk — lighter dessert choice"),
        f("Shrikhand", "1 bowl", 260, 8, 34, 10, T, "Strained-yogurt sweet — protein-rich indulgence"),
        f("Sooji Halwa", "1 bowl", 280, 5, 42, 11, T, "Semolina dessert — quick energy")
    )
}
