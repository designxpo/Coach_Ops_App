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

data class BrandSummary(
    val brand: String,
    val productCount: Int,
    val categories: List<SupplementCategory>,
)

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

    // ── Brand → Category → Product hierarchy ─────────────────────────────────

    /** All brands, alphabetical, with how many products each carries. */
    fun brands(): List<BrandSummary> =
        PRODUCTS.groupBy { it.brand }
            .map { (brand, list) ->
                BrandSummary(
                    brand = brand,
                    productCount = list.size,
                    categories = list.map { it.category }.distinct()
                        .sortedBy { SupplementCategory.entries.indexOf(it) }
                )
            }
            .sortedBy { it.brand.lowercase() }

    /** Brands matching a free-text query (brand name or any product they carry). */
    fun searchBrands(query: String): List<BrandSummary> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return brands()
        return brands().filter { b ->
            b.brand.lowercase().contains(q) ||
                PRODUCTS.any { it.brand == b.brand && it.name.lowercase().contains(q) }
        }
    }

    /** Categories a given brand offers, in enum order. */
    fun categoriesForBrand(brand: String): List<SupplementCategory> =
        PRODUCTS.filter { it.brand == brand }.map { it.category }.distinct()
            .sortedBy { SupplementCategory.entries.indexOf(it) }

    /** Products for a brand within one category. */
    fun productsFor(brand: String, category: SupplementCategory): List<SupplementProduct> =
        PRODUCTS.filter { it.brand == brand && it.category == category }.sortedBy { it.name }

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
        SupplementProduct("Applied Nutrition", "Critical Whey", SupplementCategory.WHEY, "1 serving / 2 scoops (33 g)", 33f, 125, 24f, 3.4f, 2f, "Whey protein concentrate & isolate, whey peptides, glutamine, emulsifier (soy lecithin), flavoring, sucralose; 4.8g BCAAs", "Chocolate"),
        SupplementProduct("AS-IT-IS", "ATOM Whey Protein", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 135, 25f, 4f, 2f, "Whey protein concentrate, whey protein isolate, DigeSpeed enzymes, cocoa, sucralose", "Chocolate"),
        SupplementProduct("AS-IT-IS", "Raw Whey Concentrate 80%", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 117, 24f, 1.8f, 1.5f, "100% whey protein concentrate (Agropur), soy lecithin", "Unflavoured"),
        SupplementProduct("Avvatar", "Whey Protein", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 130, 25.6f, 2.9f, 1.9f, "Fresh whey protein concentrate, cocoa, natural flavour, stevia", "Belgian Chocolate"),
        SupplementProduct("Beast Life", "Raw Whey Protein", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 132, 24f, 4f, 2f, "Whey protein concentrate, whey protein isolate, cocoa, digestive enzymes, sucralose", "Chocolate"),
        SupplementProduct("BigMuscles", "Nitric Whey", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 130, 24f, 4f, 2f, "Whey protein concentrate, whey isolate, cocoa, L-arginine, enzymes, sucralose", "Chocolate"),
        SupplementProduct("BigMuscles", "Premium Gold Whey", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 130, 25f, 3f, 1.5f, "Whey protein concentrate, whey isolate, cocoa, digestive enzymes, sucralose", "Chocolate"),
        SupplementProduct("BSN", "Syntha-6", SupplementCategory.WHEY, "1 scoop (47 g)", 47f, 200, 22f, 15f, 6f, "Sustained-release protein blend (whey concentrate & isolate, casein, milk protein isolate, egg albumen), MCTs, fiber", "Chocolate Milkshake"),
        SupplementProduct("Cellucor", "COR-Performance Whey", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 130, 25f, 3f, 1.5f, "Whey protein isolate & concentrate, cocoa, digestive enzyme blend", "Molten Chocolate"),
        SupplementProduct("Dymatize", "Elite 100% Whey", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 140, 25f, 4f, 3f, "Whey protein concentrate, whey protein isolate, whey peptides (milk), cocoa, natural & artificial flavors, sucralose", "Rich Chocolate"),
        SupplementProduct("Fast&Up", "Whey Protein", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 130, 25f, 3f, 2f, "Whey protein concentrate, whey isolate, cocoa, natural flavour, sucralose", "Chocolate"),
        SupplementProduct("GNC", "Pro Performance 100% Whey Protein", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 130, 24f, 4.5f, 1.5f, "Whey protein concentrate, whey protein isolate, cocoa, emulsifier (soy lecithin), DigeZyme enzyme blend, sucralose", "Chocolate Fudge"),
        SupplementProduct("Labrada", "100% Whey Protein", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 125, 25f, 3f, 1.5f, "Whey protein concentrate, whey protein isolate, cocoa, L-carnitine, CLA, natural flavors, lecithin, sucralose", "Chocolate"),
        SupplementProduct("MuscleBlaze", "100% Clean Raw Whey Protein Concentrate", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 118, 24f, 1.8f, 1.5f, "Whey protein concentrate, DigeZyme; unflavoured, no added sugar or maltodextrin", "Unflavoured"),
        SupplementProduct("MuscleBlaze", "Beginner's Whey Protein", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 112, 12f, 13f, 1.5f, "Whey protein concentrate, skimmed milk powder, DigeZyme", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Biozyme Performance Whey", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 140, 25f, 6f, 1.8f, "Whey protein isolate & concentrate blend, creatine monohydrate, DigeZyme, AstraGin, EAF absorption formula", "Rich Chocolate"),
        SupplementProduct("MuscleBlaze", "Fuel One Whey Protein", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 133, 24f, 5f, 2.2f, "Whey protein concentrate & isolate, glutamic acid; no added sugar or maltodextrin", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Raw Whey Protein", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 116, 24f, 1.7f, 1.5f, "100% whey protein concentrate (80%), instantised with soy lecithin", "Unflavoured"),
        SupplementProduct("MusclePharm", "Combat 100% Whey", SupplementCategory.WHEY, "1 scoop (32 g)", 32f, 130, 25f, 3f, 1.5f, "Whey protein isolate & concentrate, cocoa, sunflower lecithin, natural & artificial flavor, sucralose", "Chocolate Milk"),
        SupplementProduct("MusclePharm", "Combat Protein Powder", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 130, 25f, 5f, 2f, "Multi-source protein blend (whey concentrate, isolate, hydrolyzed whey, egg albumin, micellar casein), cocoa, MCTs, flavor, sucralose", "Chocolate Milk"),
        SupplementProduct("MuscleTech", "NitroTech", SupplementCategory.WHEY, "1 scoop (46 g)", 46f, 160, 30f, 4f, 3f, "Whey protein isolate & whey peptides, 3g creatine monohydrate, added BCAAs and glutamine, cocoa", "Milk Chocolate"),
        SupplementProduct("MuscleTech", "NitroTech (Performance Series)", SupplementCategory.WHEY, "1 scoop (46 g)", 46f, 160, 30f, 4f, 2.5f, "Whey protein isolate, whey peptides, whey protein concentrate, creatine monohydrate, cocoa, lecithin, sucralose", "Milk Chocolate"),
        SupplementProduct("MuscleTech", "NitroTech Whey Gold", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 130, 24f, 3f, 2.5f, "Whey protein isolate, whey peptides, whey concentrate, cocoa, digestive enzymes", "Double Rich Chocolate"),
        SupplementProduct("MuscleXP", "Premium Whey Protein", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 128, 24f, 3.5f, 2f, "Whey protein concentrate, whey isolate, cocoa, digestive enzymes, sucralose", "Chocolate"),
        SupplementProduct("MyProtein", "Impact Whey Protein", SupplementCategory.WHEY, "1 scoop (25 g)", 25f, 103, 21f, 1f, 1.9f, "Whey protein concentrate (milk, emulsifier: soy lecithin), cocoa, flavoring, sweetener (sucralose)", "Chocolate Smooth (most popular)"),
        SupplementProduct("MyProtein", "THE Whey", SupplementCategory.WHEY, "1 scoop (29 g)", 29f, 118, 25f, 2f, 1.5f, "Whey protein blend (isolate, hydrolysate, concentrate; milk), digestive enzymes, flavoring, sweetener (sucralose)", "Salted Caramel"),
        SupplementProduct("Nakpro", "Gold Whey Protein", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 126, 25.5f, 2f, 1.5f, "Whey protein isolate, whey protein concentrate, cocoa, natural flavour, sucralose", "Chocolate"),
        SupplementProduct("Nakpro", "Perform Whey Concentrate", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 130, 24f, 4.5f, 2f, "Whey protein concentrate, cocoa, natural flavour, sucralose", "Chocolate"),
        SupplementProduct("Nutrabay", "Gold 100% Whey Concentrate", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 120, 24f, 2f, 1.5f, "Whey protein concentrate, cocoa, digestive enzymes, natural flavour, sucralose", "Chocolate"),
        SupplementProduct("Nutrabay", "Pure 100% Whey Concentrate", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 117, 24f, 1.8f, 1.5f, "100% whey protein concentrate, soy lecithin", "Unflavoured"),
        SupplementProduct("Optimum Nutrition", "Gold Standard 100% Whey", SupplementCategory.WHEY, "1 scoop (30.4 g)", 30.4f, 120, 24f, 3f, 1.5f, "Whey protein isolate, whey protein concentrate, whey peptides, cocoa, natural & artificial flavor, lecithin, sucralose, acesulfame potassium", "Double Rich Chocolate"),
        SupplementProduct("Optimum Nutrition", "Performance Whey", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 123, 24f, 3f, 1.5f, "Whey protein concentrate & isolate, DigeZyme digestive enzyme blend, cocoa, natural & artificial flavor, lecithin, sucralose", "Chocolate Milkshake"),
        SupplementProduct("Rule One", "R1 Whey Blend", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 150, 24f, 6f, 3f, "Whey protein concentrate & isolate blend, cocoa, natural & artificial flavor, sunflower lecithin, sucralose", "Chocolate Fudge"),
        SupplementProduct("Scitec Nutrition", "100% Whey Protein Professional", SupplementCategory.WHEY, "1 serving (30 g)", 30f, 112, 22f, 3.5f, 1.6f, "Whey protein concentrate, whey protein isolate, cocoa, L-leucine, taurine, flavourings, sweeteners (sucralose, acesulfame K)", "Chocolate"),
        SupplementProduct("Scitron", "Advance Whey Protein", SupplementCategory.WHEY, "1 scoop (33.5 g)", 33.5f, 130, 25.5f, 2.6f, 1.9f, "Whey protein isolate, whey protein concentrate, DigeZyme, cocoa, sucralose", "Chocolate"),
        SupplementProduct("TrueBasics", "100% Whey Protein", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 137, 25.5f, 4f, 2f, "Whey protein isolate, whey concentrate, DigeZyme, cocoa, sucralose", "Chocolate"),
        SupplementProduct("Ultimate Nutrition", "Prostar 100% Whey Protein", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 120, 25f, 2f, 1f, "Whey protein isolate, whey protein concentrate, whey peptides, cocoa, natural flavors, lecithin, sucralose", "Chocolate Creme"),
        // ── ISOLATE ──
        SupplementProduct("Applied Nutrition", "ISO-XP", SupplementCategory.ISOLATE, "1 scoop (25 g)", 25f, 93, 22f, 0.1f, 0.2f, "100% whey protein isolate, flavoring, emulsifier (sunflower lecithin), sucralose; ultra-low lactose", "Vanilla"),
        SupplementProduct("AS-IT-IS", "Raw Whey Isolate 90%", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 113, 27f, 0.3f, 0.3f, "100% whey protein isolate, soy lecithin", "Unflavoured"),
        SupplementProduct("Avvatar", "Isorich Whey", SupplementCategory.ISOLATE, "1 scoop (34.5 g)", 34.5f, 132, 27f, 2f, 1f, "Whey protein isolate, whey protein concentrate, cocoa, sucralose", "Chocolate"),
        SupplementProduct("Beast Life", "Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 115, 26f, 1f, 0.5f, "Whey protein isolate, cocoa, natural flavour, digestive enzymes, sucralose", "Chocolate"),
        SupplementProduct("BSN", "Syntha-6 Isolate", SupplementCategory.ISOLATE, "1 scoop (34 g)", 34f, 150, 25f, 7f, 2f, "Whey protein isolate and milk protein isolate blend, 11g EAAs, cocoa", "Chocolate Milkshake"),
        SupplementProduct("Dymatize", "ISO100 Clear", SupplementCategory.ISOLATE, "1 scoop (25 g)", 25f, 90, 20f, 1f, 0f, "Hydrolyzed whey protein isolate (milk), citric acid, natural flavors, sucralose", "Orange Mango"),
        SupplementProduct("Dymatize", "ISO100 Hydrolyzed", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 120, 25f, 2f, 1f, "Hydrolyzed whey protein isolate, whey protein isolate (milk), cocoa, flavors, sucralose", "Gourmet Chocolate"),
        SupplementProduct("Isopure", "Isopure Infusions Clear Whey Isolate", SupplementCategory.ISOLATE, "1 scoop (23 g)", 23f, 90, 20f, 1f, 0f, "Whey protein isolate, natural flavor, citric acid, stevia leaf extract", "Tropical Punch"),
        SupplementProduct("Isopure", "Low Carb 100% Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 130, 25f, 3f, 1.5f, "Whey protein isolate, vitamin & mineral blend, cocoa, natural flavors, soy lecithin, sucralose", "Dutch Chocolate"),
        SupplementProduct("Isopure", "Low Carb Whey Isolate", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 110, 25f, 1f, 0.5f, "Whey protein isolate, glutamine, vitamins & minerals, sucralose", "Dutch Chocolate"),
        SupplementProduct("Isopure", "Whey Protein Isolate Unflavored", SupplementCategory.ISOLATE, "1 scoop (29 g)", 29f, 100, 25f, 0f, 0f, "100% whey protein isolate, vitamin & mineral blend, soy lecithin", "Unflavored"),
        SupplementProduct("Isopure", "Zero Carb 100% Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (31 g)", 31f, 110, 25f, 0f, 0.5f, "Whey protein isolate, vitamin & mineral blend, natural flavors, salt, soy lecithin, sucralose", "Creamy Vanilla"),
        SupplementProduct("Isopure", "Zero Carb Whey Isolate", SupplementCategory.ISOLATE, "1 scoop (31 g)", 31f, 100, 25f, 0f, 0f, "Whey protein isolate, glutamine, vitamin & mineral blend, sucralose", "Dutch Chocolate"),
        SupplementProduct("Kaged", "Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (34 g)", 34f, 130, 25f, 3f, 1.5f, "Whey protein isolate, cocoa, natural flavors, sunflower lecithin, stevia, sucralose", "Chocolate"),
        SupplementProduct("Kaged", "Whey Protein Isolate (MicroPure)", SupplementCategory.ISOLATE, "1 scoop (~33 g)", 33f, 120, 25f, 3f, 1f, "Ultra-filtered whey protein isolate, ProHydrolase digestive enzymes, natural flavor, sunflower lecithin, stevia; 5.5g BCAAs", "Vanilla"),
        SupplementProduct("Labrada", "ISO LeanPro 100% Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 115, 25f, 1f, 1f, "100% whey protein isolate, cocoa, natural flavors, lecithin, sucralose", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Biozyme Iso-Zero", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 115, 27f, 1f, 0.5f, "Whey protein isolate, EAF enhanced absorption formula, DigeZyme, sucralose", "Low Carb Ice Cream Chocolate"),
        SupplementProduct("MuscleBlaze", "Biozyme Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (31 g)", 31f, 111, 25f, 1.9f, 0.4f, "Whey protein isolate, EAF enhanced absorption formula, DigeZyme, sucralose", "Ice Cream Chocolate"),
        SupplementProduct("MuscleBlaze", "Whey Gold", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 110, 25f, 1.6f, 0.5f, "Whey protein isolate (primary), whey protein concentrate, DigeZyme", "Rich Milk Chocolate"),
        SupplementProduct("MuscleTech", "Nitro-Tech ISO Whey", SupplementCategory.ISOLATE, "1 scoop (31 g)", 31f, 110, 25f, 1f, 0.5f, "Pure whey protein isolate and whey peptides, 11g EAAs, 5g BCAAs, cocoa", "Milk Chocolate"),
        SupplementProduct("MuscleTech", "NitroTech 100% ISO Whey", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 110, 25f, 1f, 0.5f, "Whey protein isolate, whey peptides, natural & artificial flavors, sucralose", "Vanilla"),
        SupplementProduct("MyProtein", "Clear Whey Isolate", SupplementCategory.ISOLATE, "1 scoop (25 g)", 25f, 87, 20f, 0.5f, 0.1f, "Hydrolyzed whey protein isolate (milk), acidity regulators, natural flavoring, sweetener (sucralose)", "Lemonade"),
        SupplementProduct("MyProtein", "Impact Whey Isolate", SupplementCategory.ISOLATE, "1 scoop (25 g)", 25f, 93, 22f, 0.6f, 0.1f, "Whey protein isolate (milk, emulsifier: soy lecithin), flavoring, sweetener (sucralose)", "Vanilla"),
        SupplementProduct("Optimum Nutrition", "Gold Standard 100% Isolate", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 110, 25f, 1f, 0.5f, "Hydrolyzed & ultra-filtered whey protein isolate, cocoa, natural & artificial flavor, lecithin, sucralose, acesulfame potassium", "Chocolate Bliss"),
        SupplementProduct("Optimum Nutrition", "Platinum Hydrowhey", SupplementCategory.ISOLATE, "1 scoop (39 g)", 39f, 140, 30f, 3f, 1f, "Hydrolyzed whey protein isolate, added BCAAs (leucine, isoleucine, valine), cocoa, natural & artificial flavor, lecithin, sucralose", "Turbo Chocolate"),
        SupplementProduct("Rule One", "R1 Protein (Whey Isolate)", SupplementCategory.ISOLATE, "1 scoop (31 g)", 31f, 110, 25f, 1f, 0.5f, "Whey protein isolate & hydrolysate, sunflower lecithin, natural & artificial flavor, sucralose; 6g BCAAs", "Vanilla Creme"),
        SupplementProduct("Rule One", "R1 Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (33 g)", 33f, 120, 25f, 2f, 0.5f, "Hydrolyzed whey isolate, whey protein isolate, natural & artificial flavor, sucralose", "Chocolate Fudge"),
        SupplementProduct("Ultimate Nutrition", "ISO Sensation 93", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 130, 30f, 1f, 1f, "Whey protein isolate (CFM), lactase & protease enzymes, cocoa, natural flavors, lecithin, sucralose", "Chocolate Fudge"),
        SupplementProduct("Wellcore", "100% Whey Isolate", SupplementCategory.ISOLATE, "1 scoop (34 g)", 34f, 128, 27f, 1f, 1f, "Whey protein isolate, cocoa, digestive enzymes, natural flavour, sucralose", "Chocolate"),
        // ── CASEIN ──
        SupplementProduct("Dymatize", "Elite Casein", SupplementCategory.CASEIN, "2 scoops (36 g)", 36f, 130, 25f, 3f, 2f, "100% micellar casein (milk), sunflower creamer, cocoa, natural & artificial flavors, sucralose", "Rich Chocolate"),
        SupplementProduct("MuscleBlaze", "100% Micellar Casein", SupplementCategory.CASEIN, "1 scoop (35 g)", 35f, 125, 24f, 5.7f, 0.8f, "Micellar casein (slow-release), DigeZyme, cocoa powder", "Chocolate"),
        SupplementProduct("MyProtein", "Slow-Release Casein", SupplementCategory.CASEIN, "1 scoop (30 g)", 30f, 110, 24f, 2f, 1f, "Micellar casein (milk), flavoring, sweetener (sucralose)", "Chocolate"),
        SupplementProduct("Optimum Nutrition", "Gold Standard 100% Casein", SupplementCategory.CASEIN, "1 scoop (33 g)", 33f, 120, 24f, 3f, 1f, "Micellar casein, cocoa, natural & artificial flavor, lecithin, cellulose gum, sucralose, acesulfame potassium", "Creamy Vanilla"),
        SupplementProduct("Rule One", "R1 Casein", SupplementCategory.CASEIN, "1 scoop (36 g)", 36f, 120, 25f, 2f, 1f, "100% micellar casein, cocoa, natural & artificial flavor, sunflower lecithin, sucralose", "Chocolate Fudge"),
        // ── PLANT ──
        SupplementProduct("Amway", "Nutrilite All Plant Protein", SupplementCategory.PLANT, "3 tbsp (10 g)", 10f, 40, 8f, 1f, 0.3f, "Soy protein isolate, wheat protein, yellow pea protein", "Unflavoured"),
        SupplementProduct("bGREEN", "Plant Protein", SupplementCategory.PLANT, "1 scoop (37 g)", 37f, 140, 25f, 3f, 3.5f, "Pea protein isolate, brown rice protein, DigeZyme, cocoa, stevia", "Chocolate"),
        SupplementProduct("Garden of Life", "Sport Organic Plant Protein", SupplementCategory.PLANT, "1 scoop (43 g)", 43f, 170, 30f, 6f, 3f, "Organic pea protein, organic navy bean, lentil, garbanzo, probiotics", "Chocolate"),
        SupplementProduct("MuscleBlaze", "bGREEN Plant Protein", SupplementCategory.PLANT, "1 scoop (36 g)", 36f, 140, 22f, 7.3f, 2.8f, "Pea protein isolate, brown rice protein, DigeZyme; vegan, no soy/gluten/lactose", "Chocolate"),
        SupplementProduct("MyProtein", "Vegan Protein Blend", SupplementCategory.PLANT, "1 scoop (30 g)", 30f, 110, 21f, 2f, 2.5f, "Pea protein isolate, brown rice protein, flavoring, sweetener (sucralose)", "Chocolate"),
        SupplementProduct("Optimum Nutrition", "Gold Standard 100% Plant", SupplementCategory.PLANT, "1 scoop (36 g)", 36f, 150, 24f, 6f, 3f, "Pea protein, rice protein, fava bean protein, cocoa, natural flavor, sea salt, stevia, no artificial colors/flavors/sweeteners", "Rich Chocolate"),
        SupplementProduct("Origin Nutrition", "100% Vegan Plant Protein", SupplementCategory.PLANT, "1 scoop (37 g)", 37f, 140, 25f, 3f, 3f, "Pea protein isolate, brown rice protein, natural flavour, stevia", "Rich Chocolate"),
        SupplementProduct("OZiva", "Protein & Herbs for Men", SupplementCategory.PLANT, "1 scoop (33 g)", 33f, 120, 22f, 3f, 1.5f, "Pea protein, brown rice protein, mung bean protein, ashwagandha, shatavari, stevia", "Chocolate"),
        SupplementProduct("Plix", "Plant Protein", SupplementCategory.PLANT, "1 scoop (30 g)", 30f, 110, 20f, 3f, 2f, "Pea protein isolate, natural flavour, stevia", "Chocolate"),
        SupplementProduct("Vega", "Sport Premium Protein", SupplementCategory.PLANT, "1 scoop (44 g)", 44f, 170, 30f, 5f, 3f, "Pea protein, pumpkin seed protein, sunflower seed protein, alfalfa, stevia", "Chocolate"),
        SupplementProduct("Wellbeing Nutrition", "Superfood Plant Protein", SupplementCategory.PLANT, "1 scoop (35 g)", 35f, 130, 22f, 4f, 3f, "Pea protein, brown rice protein, pumpkin seed protein, superfoods, stevia", "Chocolate"),
        // ── GAINER ──
        SupplementProduct("Applied Nutrition", "Critical Mass", SupplementCategory.GAINER, "4 scoops (240 g)", 240f, 917, 54.9f, 153.8f, 9f, "Carb blend (maltodextrin, oat flour), whey protein concentrate, creatine, HMB, MCT, flavoring, sweetener", "Chocolate"),
        SupplementProduct("BigMuscles", "Real Mass Gainer", SupplementCategory.GAINER, "1 serving (100 g)", 100f, 384, 16f, 76f, 2.5f, "Maltodextrin, whey protein concentrate, oats, cocoa, digestive enzymes", "Chocolate"),
        SupplementProduct("BSN", "True-Mass", SupplementCategory.GAINER, "2 scoops (165 g)", 165f, 700, 50f, 90f, 17f, "Protein blend (whey concentrate & isolate, casein, milk protein), complex carbs, MCTs, 6g fiber", "Chocolate Milkshake"),
        SupplementProduct("Dymatize", "Super Mass Gainer", SupplementCategory.GAINER, "2.5 cups (333 g)", 333f, 1280, 52f, 245f, 9f, "Maltodextrin, protein blend (whey concentrate, isolate, casein; milk), creatine, cocoa, vitamins & minerals, flavors", "Rich Chocolate"),
        SupplementProduct("Labrada", "Muscle Mass Gainer", SupplementCategory.GAINER, "7 scoops (333 g)", 333f, 1284, 52f, 250f, 8f, "Maltodextrin, whey protein concentrate, whey protein isolate, calcium caseinate, creatine monohydrate, L-glutamine, cocoa", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Mass Gainer Pro", SupplementCategory.GAINER, "2 scoops (150 g)", 150f, 543, 30f, 92f, 6.5f, "Complex carbs, whey concentrate, milk protein, oats, flaxseed, digestive enzymes", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Mass Gainer Pro with Creapure", SupplementCategory.GAINER, "1 scoop (75 g)", 75f, 305, 18f, 54f, 2f, "Complex carbs, whey protein, Creapure creatine, betaine, L-glutamine, MCT oil, 21 vitamins & minerals", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Super Gainer XXL", SupplementCategory.GAINER, "1 scoop (100 g)", 100f, 377, 15f, 75f, 2f, "Maltodextrin, whey protein concentrate, skimmed milk powder, 27 vitamins & minerals, DigeZyme", "Chocolate"),
        SupplementProduct("MusclePharm", "Combat XL Mass Gainer", SupplementCategory.GAINER, "4 scoops (332 g)", 332f, 1270, 50f, 252f, 7f, "Carb blend (maltodextrin), protein blend (whey concentrate, casein), MCTs, creatine, glutamine, cocoa", "Chocolate"),
        SupplementProduct("MuscleTech", "Mass-Tech Extreme 2000", SupplementCategory.GAINER, "3 scoops (285 g)", 285f, 1030, 30f, 222f, 2.5f, "Multi-stage carb complex (maltodextrin), whey protein blend, L-leucine, creatine monohydrate", "Triple Chocolate Brownie"),
        SupplementProduct("MyProtein", "Weight Gainer Blend", SupplementCategory.GAINER, "1 serving (100 g)", 100f, 388, 31f, 50f, 6.2f, "Whey protein concentrate (milk), maltodextrin, oat flour, ground almonds, flavoring", "Chocolate Smooth"),
        SupplementProduct("Optimum Nutrition", "Serious Mass", SupplementCategory.GAINER, "2 heaping scoops (334 g)", 334f, 1250, 50f, 252f, 4.5f, "Maltodextrin, whey protein concentrate, 5g creatine monohydrate, 3g glutamine, 19 vitamins & minerals, cocoa, lecithin", "Chocolate"),
        SupplementProduct("Rule One", "R1 Mass Gainer", SupplementCategory.GAINER, "2 heaping scoops (318 g)", 318f, 1220, 40f, 250f, 7f, "Carb blend (maltodextrin, organic rice bran, pea starch), all-whey protein blend (concentrate, isolate, hydrolysate), MCTs, glutamine, 1g creatine monohydrate; 9g BCAAs", "Chocolate Fudge"),
        SupplementProduct("Ultimate Nutrition", "Muscle Juice Revolution 2600", SupplementCategory.GAINER, "4 scoops (265 g)", 265f, 1020, 56f, 170f, 13f, "Maltodextrin, Octo-PRO protein blend (whey concentrate, isolate, micellar casein, egg albumin), MCT & flax oil, glutamine, cocoa", "Chocolate"),
        // ── CREATINE ──
        SupplementProduct("Applied Nutrition", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 serving (5 g)", 5f, 0, 0f, 0f, 0f, "100% pure micronized creatine monohydrate, no fillers or sweeteners; Informed-Sport tested", "Unflavoured"),
        SupplementProduct("AS-IT-IS", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3.4 g)", 3.4f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate", "Unflavoured"),
        SupplementProduct("Beast Life", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "Micronized creatine monohydrate", "Unflavoured"),
        SupplementProduct("Dymatize", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (5 g)", 5f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate (Creapure)", "Unflavored"),
        SupplementProduct("GNC", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (5 g)", 5f, 0, 0f, 0f, 0f, "Creatine monohydrate", "Unflavoured"),
        SupplementProduct("Kaged", "C-HCl (Creatine HCl)", SupplementCategory.CREATINE, "750 mg (1 capsule)", 0.8f, 0, 0f, 0f, 0f, "Patented creatine hydrochloride (C-HCl), vegetable cellulose capsule; no fillers or dyes", "Unflavored"),
        SupplementProduct("MuscleBlaze", "Creatine Monohydrate CreAMP", SupplementCategory.CREATINE, "1 scoop (3.4 g)", 3.4f, 0, 0f, 0f, 0f, "Micronised creatine monohydrate 3 g, MB CreAbsorb blend", "Fruit Punch"),
        SupplementProduct("MusclePharm", "Creatine", SupplementCategory.CREATINE, "1 scoop (5 g)", 5f, 0, 0f, 0f, 0f, "Creatine matrix: creatine monohydrate, creatine HCl, creatine AAKG, creatine malate, magnesium creatine chelate", "Unflavored"),
        SupplementProduct("MuscleTech", "Cell-Tech", SupplementCategory.CREATINE, "1 scoop (44 g)", 44f, 150, 0f, 38f, 0f, "5g creatine (monohydrate + citrate), multi-stage carbs (maltodextrin, dextrose, ModCarb), taurine, alpha-lipoic acid", "Fruit Punch"),
        SupplementProduct("MuscleTech", "Platinum 100% Creatine", SupplementCategory.CREATINE, "1 scoop (5 g)", 5f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate (5g), HPLC-tested, no fillers", "Unflavored"),
        SupplementProduct("MyProtein", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "Creatine monohydrate (100%)", "Unflavored"),
        SupplementProduct("Optimum Nutrition", "Micronized Creatine Powder", SupplementCategory.CREATINE, "1 rounded tsp (5 g)", 5f, 0, 0f, 0f, 0f, "100% Creapure micronized creatine monohydrate, unflavored", "Unflavored"),
        SupplementProduct("Rule One", "R1 Creatine", SupplementCategory.CREATINE, "1 scoop (5 g)", 5f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate, no fillers or additives", "Unflavored"),
        SupplementProduct("Ultimate Nutrition", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 rounded tsp (5 g)", 5f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate", "Unflavored"),
        SupplementProduct("Wellcore", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "Micronized creatine monohydrate", "Unflavoured"),
        // ── BCAA_EAA ──
        SupplementProduct("Beast Life", "EAA", SupplementCategory.BCAA_EAA, "1 scoop (10 g)", 10f, 5, 0f, 1f, 0f, "Essential amino acids incl. BCAAs (2:1:1), electrolytes, flavouring, sucralose", "Watermelon"),
        SupplementProduct("BSN", "Amino X", SupplementCategory.BCAA_EAA, "1 scoop (14.5 g)", 14.5f, 5, 0f, 1f, 0f, "10g BCAA blend (2:1:1 leucine/isoleucine/valine), L-alanine, taurine, L-citrulline, vitamin D, stimulant-free", "Blue Raspberry"),
        SupplementProduct("Cellucor", "Alpha Amino", SupplementCategory.BCAA_EAA, "1 scoop (13 g)", 13f, 0, 0f, 0f, 0f, "5g BCAAs (2:1:1), EAA performance blend, taurine, coconut water powder, BetaPower betaine, electrolytes", "Icy Blue Razz"),
        SupplementProduct("Kaged", "In-Kaged", SupplementCategory.BCAA_EAA, "1 scoop (33 g)", 33f, 10, 0f, 2f, 0f, "Fermented BCAAs 2:1:1 (5g), L-citrulline 3g, CarnoSyn beta-alanine 1.6g, coconut water powder, taurine, L-tyrosine, PurCaf caffeine 124mg", "Fruit Punch"),
        SupplementProduct("MuscleBlaze", "BCAA Pro", SupplementCategory.BCAA_EAA, "1 scoop (15 g)", 15f, 7, 0f, 1f, 0f, "7 g vegan BCAAs (2:1:1 leucine/isoleucine/valine), 2.5 g L-glutamine, L-citrulline malate, electrolytes", "Watermelon"),
        SupplementProduct("MuscleBlaze", "EAA 8000", SupplementCategory.BCAA_EAA, "1 scoop (13.6 g)", 13.6f, 5, 0f, 0f, 0f, "8 g EAA blend (BCAA 2:1:1, lysine, threonine, phenylalanine, histidine, methionine, tryptophan), L-citrulline malate, coconut water powder, BioPerine", "Razz Lemonade"),
        SupplementProduct("MyProtein", "Essential BCAA 2:1:1", SupplementCategory.BCAA_EAA, "2 scoops (5 g)", 5f, 20, 0f, 0.5f, 0f, "BCAA 2:1:1 (L-leucine, L-isoleucine, L-valine), flavoring, sweetener (sucralose)", "Blue Raspberry"),
        SupplementProduct("Optimum Nutrition", "Amino Energy", SupplementCategory.BCAA_EAA, "1 scoop (9 g)", 9f, 5, 0f, 1f, 0f, "Amino acid blend, green tea & green coffee caffeine, beta-alanine, citrulline", "Fruit Fusion"),
        SupplementProduct("Optimum Nutrition", "BCAA 5000 Powder", SupplementCategory.BCAA_EAA, "1 scoop (11.4 g)", 11.4f, 25, 0f, 2f, 0f, "5g branched-chain amino acids (leucine, isoleucine, valine 2:1:1), citric acid, natural & artificial flavor, sucralose", "Fruit Punch"),
        SupplementProduct("Optimum Nutrition", "Essential Amino Energy", SupplementCategory.BCAA_EAA, "1 scoop (9 g)", 9f, 10, 0f, 2f, 0f, "5g micronized amino blend (BCAAs, taurine, arginine, citrulline, beta-alanine, glutamine), 100mg caffeine from green tea & green coffee, natural flavor, sucralose", "Watermelon"),
        SupplementProduct("Scivation", "Xtend BCAA", SupplementCategory.BCAA_EAA, "1 scoop (13.5 g)", 13.5f, 0, 0f, 1f, 0f, "L-leucine, L-isoleucine, L-valine (2:1:1), L-glutamine, citrulline malate, electrolytes", "Blue Raspberry"),
        // ── PREWORKOUT ──
        SupplementProduct("Beast Life", "Pre-Workout", SupplementCategory.PREWORKOUT, "1 scoop (10 g)", 10f, 10, 0f, 2f, 0f, "L-citrulline, beta-alanine, caffeine (200 mg), L-tyrosine, taurine", "Fruit Blast"),
        SupplementProduct("Cellucor", "C4 Original", SupplementCategory.PREWORKOUT, "1 scoop (6 g)", 6f, 5, 0f, 1f, 0f, "Beta-alanine, creatine nitrate, caffeine (150 mg), arginine AKG, N-acetyl tyrosine", "Fruit Punch"),
        SupplementProduct("Cellucor", "C4 Original Pre-Workout", SupplementCategory.PREWORKOUT, "1 scoop (6.5 g)", 6.5f, 5, 0f, 1f, 0f, "1.6g CarnoSyn beta-alanine, 1g creatine monohydrate, arginine AKG, N-acetyl-L-tyrosine, 150mg caffeine, vitamins C/B6/B12", "Fruit Punch"),
        SupplementProduct("Cellucor", "C4 Ripped Pre-Workout", SupplementCategory.PREWORKOUT, "1 scoop (5.7 g)", 5.7f, 5, 0f, 1f, 0f, "CarnoSyn beta-alanine, L-carnitine tartrate, 150mg caffeine, green coffee bean extract, Capsimax cayenne, L-tyrosine", "Cherry Limeade"),
        SupplementProduct("Kaged", "Pre-Kaged", SupplementCategory.PREWORKOUT, "1 scoop (32 g)", 32f, 20, 0f, 5f, 0f, "L-citrulline 6.5g, beta-alanine 1.6g, BCAAs, betaine, taurine, L-tyrosine, PurCaf organic caffeine 274mg, L-theanine, creatine HCl, Spectra", "Fruit Punch"),
        SupplementProduct("MuscleBlaze", "Pre Workout WrathX", SupplementCategory.PREWORKOUT, "1 scoop (17 g)", 17f, 8, 0f, 2f, 0f, "L-citrulline malate 3 g, beta-alanine, L-arginine, Creapure creatine, caffeine, L-theanine, taurine, EnXtra, BioPerine", "Fruit Fury"),
        SupplementProduct("Optimum Nutrition", "Gold Standard Pre-Workout", SupplementCategory.PREWORKOUT, "1 scoop (13.4 g)", 13.4f, 5, 0f, 3f, 0f, "3g creatine monohydrate, 1.6g CarnoSyn beta-alanine, 175mg caffeine, 375mg N-acetyl-L-tyrosine, AstraGin, natural flavor, sucralose", "Fruit Punch"),
        // ── BAR ──
        SupplementProduct("Barebells", "Protein Bar", SupplementCategory.BAR, "1 bar (55 g)", 55f, 200, 20f, 16f, 8f, "Milk protein, soy protein, humectants, cocoa, almonds, sucralose", "Cookies & Cream"),
        SupplementProduct("Grenade", "Carb Killa", SupplementCategory.BAR, "1 bar (60 g)", 60f, 214, 20f, 17f, 8f, "Milk protein, whey protein, collagen, cocoa, humectants, sucralose", "Chocolate Chip Salted Caramel"),
        SupplementProduct("MuscleBlaze", "Hi-Protein Bar (30g Protein)", SupplementCategory.BAR, "1 bar (70 g)", 70f, 350, 30f, 46f, 6.5f, "Milk protein blend, whey protein, soluble fibre, cocoa, almonds, sweeteners", "Choco Almond"),
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
        SupplementProduct("MuscleBlaze", "High Protein Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (34 g)", 34f, 193, 9f, 13f, 13.6f, "Roasted peanuts, whey protein concentrate, cocoa, sweetener", "Dark Chocolate"),
        SupplementProduct("MyFitness", "Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (33 g)", 33f, 190, 8f, 9f, 14f, "Roasted peanuts, cocoa, cane sugar, sea salt", "Chocolate"),
        SupplementProduct("Pintola", "All Natural Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (32 g)", 32f, 190, 8f, 6f, 15f, "100% roasted peanuts", "Creamy"),
        SupplementProduct("Pintola", "High Protein Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (32 g)", 32f, 180, 11f, 6f, 13f, "Roasted peanuts, whey protein concentrate", "Creamy"),
        SupplementProduct("The Whole Truth", "Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (32 g)", 32f, 190, 9f, 6f, 15f, "Roasted peanuts, no added sugar or palm oil", "Classic"),
        // ── PROTEIN_FOOD ──
        SupplementProduct("Labrada", "Lean Body Hi-Protein Meal Replacement", SupplementCategory.PROTEIN_FOOD, "2 scoops (70 g)", 70f, 285, 35f, 20f, 7f, "Protein blend (whey concentrate, calcium caseinate, egg albumin), sunflower oil, dietary fiber, vitamin & mineral blend, cocoa", "Chocolate"),
        SupplementProduct("MuscleBlaze", "High Protein Oats", SupplementCategory.PROTEIN_FOOD, "100 g", 100f, 356, 19f, 41f, 14.4f, "Rolled oats, whey protein, fruits & seeds, probiotics, dietary fibre", "Dark Chocolate"),
        SupplementProduct("True Elements", "Protein Muesli", SupplementCategory.PROTEIN_FOOD, "1 serving (40 g)", 40f, 160, 9f, 24f, 3f, "Rolled oats, soy protein, nuts, seeds, raisins", "Original"),
        SupplementProduct("Yoga Bar", "Protein Oats", SupplementCategory.PROTEIN_FOOD, "1 serving (50 g)", 50f, 190, 10f, 32f, 3f, "Rolled oats, soy protein, almonds, cocoa", "Chocolate"),
        // ── RTD ──
        SupplementProduct("Amul", "High Protein Buttermilk", SupplementCategory.RTD, "1 pack (200 ml)", 200f, 60, 10f, 5f, 0f, "Toned milk, milk solids, active cultures, salt, spices", "Chaas"),
        SupplementProduct("Amul", "High Protein Lassi", SupplementCategory.RTD, "1 bottle (200 ml)", 200f, 140, 15f, 18f, 0.5f, "Toned milk, milk solids, sugar, active cultures", "Lassi"),
        SupplementProduct("Amul", "High Protein Milk", SupplementCategory.RTD, "1 pack (250 ml)", 250f, 140, 15f, 18f, 0.5f, "Toned milk, milk protein concentrate", "Plain"),
    )
}
