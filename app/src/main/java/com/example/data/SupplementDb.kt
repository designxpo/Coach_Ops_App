package com.example.data

/**
 * Built-in supplement database — protein powders, gainers, creatine, bars and
 * other protein products from the global + Indian brands users actually buy,
 * with per-serving label nutrition and key ingredients.
 *
 * Works fully offline. Logged servings flow into the Food Diary as normal
 * entries, so protein intake is counted automatically by the existing macro
 * totals. Values are per label for the most popular flavor — treat as
 * approximate (brands revise labels).
 */

enum class SupplementCategory(val label: String, val emoji: String) {
    WHEY("Whey", "🥛"),
    ISOLATE("Isolate", "💎"),
    CASEIN("Casein", "🌙"),
    PLANT("Plant", "🌱"),
    GAINER("Gainer", "🏋️"),
    CREATINE("Creatine", "⚡"),
    BCAA_EAA("BCAA/EAA", "🧬"),
    PREWORKOUT("Pre-workout", "🔥"),
    BAR("Bars", "🍫"),
    PEANUT_BUTTER("Nut Butter", "🥜"),
    PROTEIN_FOOD("Protein Foods", "🥣"),
    RTD("Drinks", "🧃"),
}

data class SupplementProduct(
    val brand: String,
    val name: String,
    val category: SupplementCategory,
    val servingDesc: String,   // "1 scoop (30.4 g)"
    val servingG: Float,
    val calories: Int,         // per serving
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val ingredients: String,   // key label ingredients
    val flavorNote: String = "",
)

object SupplementDb {

    /**
     * Token search over brand + product + category. Every query token must
     * match somewhere, so "mb whey" or "muscleblaze biozyme" both work.
     */
    fun search(query: String, category: SupplementCategory? = null): List<SupplementProduct> {
        val base = if (category != null) PRODUCTS.filter { it.category == category } else PRODUCTS
        val tokens = query.lowercase().split(Regex("\\s+")).filter { it.length >= 2 }
        if (tokens.isEmpty()) return base
        return base.filter { p ->
            val hay = "${p.brand} ${p.name} ${p.category.label}".lowercase()
            tokens.all { hay.contains(it) }
        }.sortedWith(compareBy({ it.brand }, { it.name }))
    }

    fun byCategory(category: SupplementCategory): List<SupplementProduct> =
        PRODUCTS.filter { it.category == category }.sortedWith(compareBy({ it.brand }, { it.name }))

    /** Converts a serving count into the shared nutrition format the diary logs. */
    fun toNutrition(p: SupplementProduct, servings: Float): FoodNutrition = FoodNutrition(
        foodName    = "${p.brand} ${p.name}",
        servingSize = if (servings == 1f) p.servingDesc
                      else "${fmt(servings)} × ${p.servingDesc}",
        calories    = (p.calories * servings).toInt(),
        proteinG    = p.proteinG * servings,
        carbsG      = p.carbsG * servings,
        fatG        = p.fatG * servings,
        fiberG      = 0f,
        confidence  = "high",
        notes       = p.ingredients,
    )

    private fun fmt(f: Float): String =
        if (f == f.toInt().toFloat()) f.toInt().toString() else "%.1f".format(f)

    // ─── Product data (label values, most popular flavor) ─────────────────────
    // Seed entries below; the full generated database replaces this list.
    val PRODUCTS: List<SupplementProduct> = listOf(
        // ── WHEY ──
        SupplementProduct("AS-IT-IS", "ATOM Whey Protein", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 135, 25f, 4f, 2f, "Whey protein concentrate, whey protein isolate, DigeSpeed enzymes, cocoa, sucralose", "Chocolate"),
        SupplementProduct("AS-IT-IS", "Raw Whey Concentrate 80%", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 117, 24f, 1.8f, 1.5f, "100% whey protein concentrate (Agropur), soy lecithin", "Unflavoured"),
        SupplementProduct("Applied Nutrition", "Critical Whey", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 112, 21f, 3f, 1.5f, "Whey protein concentrate (milk), fat-reduced cocoa powder, flavouring, emulsifier (soy lecithin), sweetener (sucralose)", "Chocolate"),
        SupplementProduct("Avvatar", "Whey Protein", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 130, 25.6f, 2.9f, 1.9f, "Fresh whey protein concentrate, cocoa, natural flavour, stevia", "Belgian Chocolate"),
        SupplementProduct("BSN", "Syntha-6", SupplementCategory.WHEY, "1 scoop (47 g)", 47f, 200, 22f, 15f, 6f, "Whey concentrate & isolate, calcium caseinate, micellar casein, milk protein isolate, egg albumen, MCT powder, cocoa, sucralose", "Chocolate Milkshake"),
        SupplementProduct("BigMuscles", "Nitric Whey", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 130, 24f, 4f, 2f, "Whey protein concentrate, whey isolate, cocoa, L-arginine, enzymes, sucralose", "Chocolate"),
        SupplementProduct("BigMuscles", "Premium Gold Whey", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 130, 25f, 3f, 1.5f, "Whey protein concentrate, whey isolate, cocoa, digestive enzymes, sucralose", "Chocolate"),
        SupplementProduct("Cellucor", "COR-Performance Whey", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 130, 25f, 3f, 1.5f, "Whey protein concentrate, whey protein isolate, cocoa, natural & artificial flavors, lecithin, sucralose, digestive enzymes", "Molten Chocolate"),
        SupplementProduct("Dymatize", "Elite 100% Whey Protein", SupplementCategory.WHEY, "1 scoop (36 g)", 36f, 140, 25f, 4f, 2.5f, "Whey protein concentrate, whey protein isolate, cocoa, natural & artificial flavors, lecithin, sucralose, protease enzymes", "Rich Chocolate"),
        SupplementProduct("Fast&Up", "Whey Protein", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 130, 25f, 3f, 2f, "Whey protein concentrate, whey isolate, cocoa, natural flavour, sucralose", "Chocolate"),
        SupplementProduct("GNC", "Pro Performance 100% Whey Protein", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 130, 24f, 4.5f, 1.5f, "Whey protein concentrate, whey protein isolate, cocoa, emulsifier (soy lecithin), DigeZyme enzyme blend, sucralose", "Chocolate Fudge"),
        SupplementProduct("Labrada", "100% Whey Protein", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 140, 24f, 5f, 2.5f, "Whey protein concentrate, Dutched cocoa, natural & artificial flavors, xanthan gum, salt, acesulfame potassium, sucralose", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Biozyme Performance Whey", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 132, 25f, 3.6f, 1.5f, "Whey protein concentrate, cocoa, DigeZyme multi-enzyme complex, EnzeGut, sucralose", "Rich Chocolate"),
        SupplementProduct("MuscleBlaze", "Raw Whey Protein", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 116, 24f, 1.7f, 1.5f, "100% whey protein concentrate (80%), instantised with soy lecithin", "Unflavoured"),
        SupplementProduct("MusclePharm", "Combat Protein Powder", SupplementCategory.WHEY, "1 scoop (37 g)", 37f, 130, 25f, 5f, 1.5f, "Whey concentrate, whey isolate, hydrolyzed whey, micellar casein, egg albumin, cocoa, natural & artificial flavors, sucralose", "Chocolate Milk"),
        SupplementProduct("MuscleTech", "NitroTech (Performance Series)", SupplementCategory.WHEY, "1 scoop (46 g)", 46f, 160, 30f, 4f, 2.5f, "Whey protein isolate, whey peptides, whey protein concentrate, creatine monohydrate, cocoa, lecithin, sucralose", "Milk Chocolate"),
        SupplementProduct("MuscleTech", "NitroTech Whey Gold", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 120, 24f, 2f, 1.5f, "Whey peptides, whey protein isolate, whey protein concentrate, cocoa, natural flavors, lecithin, sucralose, digestive enzymes", "Double Rich Chocolate"),
        SupplementProduct("MuscleXP", "Premium Whey Protein", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 128, 24f, 3.5f, 2f, "Whey protein concentrate, whey isolate, cocoa, digestive enzymes, sucralose", "Chocolate"),
        SupplementProduct("MyProtein", "Impact Whey Protein", SupplementCategory.WHEY, "1 scoop (25 g)", 25f, 97, 18f, 4f, 1.9f, "Whey protein concentrate (milk), cocoa powder, emulsifier (soy lecithin), flavouring, sweetener (sucralose)", "Chocolate Smooth"),
        SupplementProduct("Nakpro", "Gold Whey Protein", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 126, 25.5f, 2f, 1.5f, "Whey protein isolate, whey protein concentrate, cocoa, natural flavour, sucralose", "Chocolate"),
        SupplementProduct("Nakpro", "Perform Whey Concentrate", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 130, 24f, 4.5f, 2f, "Whey protein concentrate, cocoa, natural flavour, sucralose", "Chocolate"),
        SupplementProduct("Nutrabay", "Gold 100% Whey Concentrate", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 120, 24f, 2f, 1.5f, "Whey protein concentrate, cocoa, digestive enzymes, natural flavour, sucralose", "Chocolate"),
        SupplementProduct("Nutrabay", "Pure 100% Whey Concentrate", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 117, 24f, 1.8f, 1.5f, "100% whey protein concentrate, soy lecithin", "Unflavoured"),
        SupplementProduct("Optimum Nutrition", "Gold Standard 100% Whey", SupplementCategory.WHEY, "1 scoop (30.4 g)", 30.4f, 120, 24f, 3f, 1f, "Whey protein isolate, whey protein concentrate, hydrolyzed whey isolate, cocoa, lecithin, acesulfame K, aminogen & lactase enzymes", "Double Rich Chocolate"),
        SupplementProduct("Rule One", "R1 Whey Blend", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 130, 24f, 4f, 2.5f, "Whey protein concentrate, whey protein isolate, hydrolyzed whey, cocoa, natural & artificial flavors, lecithin, sucralose", "Chocolate Fudge"),
        SupplementProduct("Scitec Nutrition", "100% Whey Protein Professional", SupplementCategory.WHEY, "1 serving (30 g)", 30f, 112, 22f, 3.5f, 1.6f, "Whey protein concentrate, whey protein isolate, cocoa, L-leucine, taurine, flavourings, sweeteners (sucralose, acesulfame K)", "Chocolate"),
        SupplementProduct("Scitron", "Advance Whey Protein", SupplementCategory.WHEY, "1 scoop (33.5 g)", 33.5f, 130, 25.5f, 2.6f, 1.9f, "Whey protein isolate, whey protein concentrate, DigeZyme, cocoa, sucralose", "Chocolate"),
        SupplementProduct("TrueBasics", "100% Whey Protein", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 137, 25.5f, 4f, 2f, "Whey protein isolate, whey concentrate, DigeZyme, cocoa, sucralose", "Chocolate"),
        SupplementProduct("Ultimate Nutrition", "Prostar 100% Whey Protein", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 120, 25f, 2f, 1f, "Whey protein isolate, whey protein concentrate, hydrolyzed whey peptides, cocoa, natural & artificial flavors, lecithin, sucralose", "Chocolate Creme"),
        // ── ISOLATE ──
        SupplementProduct("AS-IT-IS", "Raw Whey Isolate 90%", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 113, 27f, 0.3f, 0.3f, "100% whey protein isolate, soy lecithin", "Unflavoured"),
        SupplementProduct("Applied Nutrition", "ISO-XP", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 116, 27f, 0.7f, 0.5f, "Whey protein isolate (milk), cocoa, flavouring, sweetener (sucralose)", "Choc Dessert"),
        SupplementProduct("Avvatar", "Isorich Whey", SupplementCategory.ISOLATE, "1 scoop (34.5 g)", 34.5f, 132, 27f, 2f, 1f, "Whey protein isolate, whey protein concentrate, cocoa, sucralose", "Chocolate"),
        SupplementProduct("Dymatize", "ISO100 Hydrolyzed", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 120, 25f, 3f, 0.5f, "Hydrolyzed whey protein isolate, whey protein isolate, cocoa, sucralose", "Gourmet Chocolate"),
        SupplementProduct("Isopure", "Low Carb Whey Isolate", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 110, 25f, 1f, 0.5f, "Whey protein isolate, glutamine, vitamins & minerals, sucralose", "Dutch Chocolate"),
        SupplementProduct("Isopure", "Zero Carb Whey Isolate", SupplementCategory.ISOLATE, "1 scoop (31 g)", 31f, 100, 25f, 0f, 0f, "Whey protein isolate, glutamine, vitamin & mineral blend, sucralose", "Dutch Chocolate"),
        SupplementProduct("Kaged", "Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (34 g)", 34f, 130, 25f, 3f, 1.5f, "Whey protein isolate, cocoa, natural flavors, sunflower lecithin, stevia, sucralose", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Biozyme Iso-Zero", SupplementCategory.ISOLATE, "1 scoop (33.5 g)", 33.5f, 120, 27f, 0f, 0.5f, "Whey protein isolate, MCT, DigeZyme, cocoa, sucralose", "Chocolate"),
        SupplementProduct("MuscleTech", "NitroTech 100% ISO Whey", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 110, 25f, 1f, 0.5f, "Whey protein isolate, whey peptides, natural & artificial flavors, sucralose", "Vanilla"),
        SupplementProduct("MyProtein", "Impact Whey Isolate", SupplementCategory.ISOLATE, "1 scoop (25 g)", 25f, 93, 21f, 0.6f, 0.5f, "Whey protein isolate (milk), flavouring, sweetener (sucralose)", "Chocolate"),
        SupplementProduct("Optimum Nutrition", "Gold Standard 100% Isolate", SupplementCategory.ISOLATE, "1 scoop (31 g)", 31f, 110, 25f, 1f, 0.5f, "Whey protein isolate, natural & artificial flavor, lecithin, sucralose", "Rich Vanilla"),
        SupplementProduct("Rule One", "R1 Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (33 g)", 33f, 120, 25f, 2f, 0.5f, "Hydrolyzed whey isolate, whey protein isolate, natural & artificial flavor, sucralose", "Chocolate Fudge"),
        SupplementProduct("Wellcore", "100% Whey Isolate", SupplementCategory.ISOLATE, "1 scoop (34 g)", 34f, 128, 27f, 1f, 1f, "Whey protein isolate, cocoa, digestive enzymes, natural flavour, sucralose", "Chocolate"),
        // ── CASEIN ──
        SupplementProduct("Dymatize", "Elite Casein", SupplementCategory.CASEIN, "1 scoop (36 g)", 36f, 130, 25f, 3f, 1.5f, "Micellar casein, cocoa, natural & artificial flavors, lecithin, sucralose", "Rich Chocolate"),
        SupplementProduct("MyProtein", "Slow-Release Casein", SupplementCategory.CASEIN, "1 scoop (30 g)", 30f, 108, 24f, 1.5f, 0.5f, "Micellar casein (milk), flavouring, sweetener (sucralose)", "Chocolate"),
        SupplementProduct("Optimum Nutrition", "Gold Standard 100% Casein", SupplementCategory.CASEIN, "1 scoop (34 g)", 34f, 120, 24f, 3f, 1f, "Micellar casein, cocoa, lecithin, natural & artificial flavor, sucralose", "Chocolate Supreme"),
        // ── PLANT ──
        SupplementProduct("Amway", "Nutrilite All Plant Protein", SupplementCategory.PLANT, "3 tbsp (10 g)", 10f, 40, 8f, 1f, 0.3f, "Soy protein isolate, wheat protein, yellow pea protein", "Unflavoured"),
        SupplementProduct("Garden of Life", "Sport Organic Plant Protein", SupplementCategory.PLANT, "1 scoop (43 g)", 43f, 170, 30f, 6f, 3f, "Organic pea protein, organic navy bean, lentil, garbanzo, probiotics", "Chocolate"),
        SupplementProduct("MyProtein", "Vegan Protein Blend", SupplementCategory.PLANT, "1 scoop (30 g)", 30f, 111, 22f, 2.4f, 2.3f, "Pea protein isolate, brown rice protein, hemp protein, flavouring, stevia", "Chocolate"),
        SupplementProduct("OZiva", "Protein & Herbs for Men", SupplementCategory.PLANT, "1 scoop (33 g)", 33f, 120, 22f, 3f, 1.5f, "Pea protein, brown rice protein, mung bean protein, ashwagandha, shatavari, stevia", "Chocolate"),
        SupplementProduct("Optimum Nutrition", "Gold Standard 100% Plant", SupplementCategory.PLANT, "1 scoop (37 g)", 37f, 150, 24f, 5f, 3f, "Pea protein, brown rice protein, sacha inchi protein, cocoa, stevia", "Chocolate"),
        SupplementProduct("Origin Nutrition", "100% Vegan Plant Protein", SupplementCategory.PLANT, "1 scoop (37 g)", 37f, 140, 25f, 3f, 3f, "Pea protein isolate, brown rice protein, natural flavour, stevia", "Rich Chocolate"),
        SupplementProduct("Plix", "Plant Protein", SupplementCategory.PLANT, "1 scoop (30 g)", 30f, 110, 20f, 3f, 2f, "Pea protein isolate, natural flavour, stevia", "Chocolate"),
        SupplementProduct("Vega", "Sport Premium Protein", SupplementCategory.PLANT, "1 scoop (44 g)", 44f, 170, 30f, 5f, 3f, "Pea protein, pumpkin seed protein, sunflower seed protein, alfalfa, stevia", "Chocolate"),
        SupplementProduct("Wellbeing Nutrition", "Superfood Plant Protein", SupplementCategory.PLANT, "1 scoop (35 g)", 35f, 130, 22f, 4f, 3f, "Pea protein, brown rice protein, pumpkin seed protein, superfoods, stevia", "Chocolate"),
        SupplementProduct("bGREEN", "Plant Protein", SupplementCategory.PLANT, "1 scoop (37 g)", 37f, 140, 25f, 3f, 3.5f, "Pea protein isolate, brown rice protein, DigeZyme, cocoa, stevia", "Chocolate"),
        // ── GAINER ──
        SupplementProduct("BigMuscles", "Real Mass Gainer", SupplementCategory.GAINER, "1 serving (100 g)", 100f, 384, 16f, 76f, 2.5f, "Maltodextrin, whey protein concentrate, oats, cocoa, digestive enzymes", "Chocolate"),
        SupplementProduct("Dymatize", "Super Mass Gainer", SupplementCategory.GAINER, "2 scoops (336 g)", 336f, 1280, 52f, 245f, 9f, "Maltodextrin, whey protein concentrate, whey isolate, casein, cocoa, enzymes", "Rich Chocolate"),
        SupplementProduct("MuscleBlaze", "Mass Gainer Pro", SupplementCategory.GAINER, "2 scoops (150 g)", 150f, 543, 30f, 92f, 6.5f, "Complex carbs, whey concentrate, milk protein, oats, flaxseed, digestive enzymes", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Super Gainer XXL", SupplementCategory.GAINER, "1 serving (100 g)", 100f, 375, 20f, 70f, 2.5f, "Maltodextrin, whey protein concentrate, milk solids, cocoa, digestive enzymes", "Chocolate"),
        SupplementProduct("Optimum Nutrition", "Serious Mass", SupplementCategory.GAINER, "2 scoops (334 g)", 334f, 1250, 50f, 252f, 4.5f, "Maltodextrin, whey protein concentrate, calcium caseinate, egg albumen, cocoa, vitamins", "Chocolate"),
        // ── CREATINE ──
        SupplementProduct("AS-IT-IS", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3.4 g)", 3.4f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate", "Unflavoured"),
        SupplementProduct("GNC", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (5 g)", 5f, 0, 0f, 0f, 0f, "Creatine monohydrate", "Unflavoured"),
        SupplementProduct("MuscleBlaze", "Creatine Monohydrate CreAMP", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "Creatine monohydrate", "Unflavoured"),
        SupplementProduct("MyProtein", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 serving (3 g)", 3f, 0, 0f, 0f, 0f, "Creatine monohydrate", "Unflavoured"),
        SupplementProduct("Optimum Nutrition", "Micronized Creatine Powder", SupplementCategory.CREATINE, "1 tsp (3.4 g)", 3.4f, 0, 0f, 0f, 0f, "Creatine monohydrate (Creapure)", "Unflavoured"),
        SupplementProduct("Wellcore", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "Micronized creatine monohydrate", "Unflavoured"),
        // ── BCAA_EAA ──
        SupplementProduct("MuscleBlaze", "BCAA Pro", SupplementCategory.BCAA_EAA, "1 scoop (7.5 g)", 7.5f, 0, 0f, 0f, 0f, "L-leucine, L-isoleucine, L-valine (2:1:1), L-glutamine, electrolytes", "Fruit Punch"),
        SupplementProduct("MyProtein", "Essential BCAA 2:1:1", SupplementCategory.BCAA_EAA, "1 scoop (5 g)", 5f, 0, 0f, 0f, 0f, "L-leucine, L-isoleucine, L-valine (2:1:1), flavouring, sucralose", "Berry Burst"),
        SupplementProduct("Optimum Nutrition", "Amino Energy", SupplementCategory.BCAA_EAA, "1 scoop (9 g)", 9f, 5, 0f, 1f, 0f, "Amino acid blend, green tea & green coffee caffeine, beta-alanine, citrulline", "Fruit Fusion"),
        SupplementProduct("Scivation", "Xtend BCAA", SupplementCategory.BCAA_EAA, "1 scoop (13.5 g)", 13.5f, 0, 0f, 1f, 0f, "L-leucine, L-isoleucine, L-valine (2:1:1), L-glutamine, citrulline malate, electrolytes", "Blue Raspberry"),
        // ── PREWORKOUT ──
        SupplementProduct("Cellucor", "C4 Original", SupplementCategory.PREWORKOUT, "1 scoop (6 g)", 6f, 5, 0f, 1f, 0f, "Beta-alanine, creatine nitrate, caffeine (150 mg), arginine AKG, N-acetyl tyrosine", "Fruit Punch"),
        SupplementProduct("Kaged", "Pre-Kaged", SupplementCategory.PREWORKOUT, "1 scoop (20 g)", 20f, 20, 0f, 5f, 0f, "Citrulline, beta-alanine, betaine, creatine HCl, caffeine (274 mg), L-tyrosine", "Fruit Punch"),
        SupplementProduct("MuscleBlaze", "PRE Workout WrathX", SupplementCategory.PREWORKOUT, "1 scoop (8 g)", 8f, 10, 0f, 2f, 0f, "Citrulline malate, beta-alanine, caffeine (200 mg), L-arginine, taurine", "Fruit Blast"),
        SupplementProduct("Optimum Nutrition", "Gold Standard Pre-Workout", SupplementCategory.PREWORKOUT, "1 scoop (12 g)", 12f, 5, 0f, 3f, 0f, "Beta-alanine, creatine monohydrate, caffeine (175 mg), citrulline, N-acetyl tyrosine", "Blueberry Lemonade"),
        // ── BAR ──
        SupplementProduct("Barebells", "Protein Bar", SupplementCategory.BAR, "1 bar (55 g)", 55f, 200, 20f, 16f, 8f, "Milk protein, soy protein, humectants, cocoa, almonds, sucralose", "Cookies & Cream"),
        SupplementProduct("Grenade", "Carb Killa", SupplementCategory.BAR, "1 bar (60 g)", 60f, 214, 20f, 17f, 8f, "Milk protein, whey protein, collagen, cocoa, humectants, sucralose", "Chocolate Chip Salted Caramel"),
        SupplementProduct("MuscleBlaze", "Protein Bar", SupplementCategory.BAR, "1 bar (60 g)", 60f, 213, 20f, 21f, 6f, "Whey protein, milk protein, soy protein, dates, nuts, cocoa", "Chocolate"),
        SupplementProduct("Quest", "Protein Bar", SupplementCategory.BAR, "1 bar (60 g)", 60f, 200, 21f, 22f, 8f, "Protein blend (whey isolate, milk isolate), almonds, soluble fibre, erythritol, stevia", "Chocolate Chip Cookie Dough"),
        SupplementProduct("RiteBite", "Max Protein Daily", SupplementCategory.BAR, "1 bar (50 g)", 50f, 180, 10f, 22f, 6f, "Soy protein, milk protein, dates, nuts, cocoa, oats", "Choco Slim"),
        SupplementProduct("RiteBite", "Max Protein Professional", SupplementCategory.BAR, "1 bar (60 g)", 60f, 232, 20f, 22f, 7f, "Whey protein, milk protein, soy protein, nuts, cocoa, dates", "Choco Whey"),
        SupplementProduct("The Whole Truth", "Protein Bar", SupplementCategory.BAR, "1 bar (52 g)", 52f, 200, 10f, 20f, 9f, "Dates, whey protein, peanuts, almonds, cocoa, no added sugar", "Chocolate Brownie"),
        SupplementProduct("Yoga Bar", "20g Protein Bar", SupplementCategory.BAR, "1 bar (65 g)", 65f, 210, 20f, 20f, 7f, "Milk protein blend, soy protein, dates, almonds, cocoa, whey protein", "Chocolate Brownie"),
        // ── PEANUT_BUTTER ──
        SupplementProduct("Alpino", "High Protein Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (32 g)", 32f, 184, 10f, 7f, 14f, "Roasted peanuts, whey protein, cocoa", "Choco"),
        SupplementProduct("Alpino", "Natural Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (32 g)", 32f, 191, 8f, 6f, 16f, "100% roasted peanuts", "Creamy"),
        SupplementProduct("DiSano", "All Natural Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (32 g)", 32f, 190, 8f, 7f, 15f, "100% roasted peanuts", "Creamy"),
        SupplementProduct("MyFitness", "Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (33 g)", 33f, 190, 8f, 9f, 14f, "Roasted peanuts, cocoa, cane sugar, sea salt", "Chocolate"),
        SupplementProduct("Pintola", "All Natural Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (32 g)", 32f, 190, 8f, 6f, 15f, "100% roasted peanuts", "Creamy"),
        SupplementProduct("Pintola", "High Protein Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (32 g)", 32f, 180, 11f, 6f, 13f, "Roasted peanuts, whey protein concentrate", "Creamy"),
        SupplementProduct("The Whole Truth", "Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (32 g)", 32f, 190, 9f, 6f, 15f, "Roasted peanuts, no added sugar or palm oil", "Classic"),
        // ── PROTEIN_FOOD ──
        SupplementProduct("MuscleBlaze", "High Protein Oats", SupplementCategory.PROTEIN_FOOD, "1 serving (50 g)", 50f, 190, 12f, 30f, 3f, "Whole grain oats, whey protein concentrate, cocoa", "Chocolate"),
        SupplementProduct("True Elements", "Protein Muesli", SupplementCategory.PROTEIN_FOOD, "1 serving (40 g)", 40f, 160, 9f, 24f, 3f, "Rolled oats, soy protein, nuts, seeds, raisins", "Original"),
        SupplementProduct("Yoga Bar", "Protein Oats", SupplementCategory.PROTEIN_FOOD, "1 serving (50 g)", 50f, 190, 10f, 32f, 3f, "Rolled oats, soy protein, almonds, cocoa", "Chocolate"),
        // ── RTD ──
        SupplementProduct("Amul", "High Protein Buttermilk", SupplementCategory.RTD, "1 pack (200 ml)", 200f, 60, 10f, 5f, 0f, "Toned milk, milk solids, active cultures, salt, spices", "Chaas"),
        SupplementProduct("Amul", "High Protein Lassi", SupplementCategory.RTD, "1 bottle (200 ml)", 200f, 140, 15f, 18f, 0.5f, "Toned milk, milk solids, sugar, active cultures", "Lassi"),
        SupplementProduct("Amul", "High Protein Milk", SupplementCategory.RTD, "1 pack (250 ml)", 250f, 140, 15f, 18f, 0.5f, "Toned milk, milk protein concentrate", "Plain"),
    )
}
