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
        SupplementProduct("AS-IT-IS", "ATOM Whey Protein", SupplementCategory.WHEY, "1 scoop (36 g)", 36f, 150, 27f, 5f, 2.5f, "Whey protein concentrate & isolate, DigeZyme digestive enzymes, cocoa, natural flavour, sweetener (sucralose)", "Double Rich Chocolate"),
        SupplementProduct("AS-IT-IS", "Raw Whey Protein Concentrate 80%", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 120, 24f, 2f, 1.8f, "Whey protein concentrate (unflavoured), instantized with soy lecithin; no sweeteners, non-GMO, gluten-free", "Unflavoured"),
        SupplementProduct("Avvatar", "Absolute 100% Performance Whey", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 139, 25f, 4f, 2f, "Whey protein concentrate & isolate, cocoa, natural flavour, sweetener (sucralose)", "Malai Kulfi"),
        SupplementProduct("Avvatar", "Whey Protein", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 141, 28f, 3f, 2.1f, "Whey protein isolate & concentrate blend (from fresh cow's milk), natural flavour, sweetener (sucralose)", "Malai Kulfi"),
        SupplementProduct("Beast Life", "Pro Concentrate Whey Protein", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 130, 24f, 4f, 2.5f, "100% grass-fed whey protein concentrate (CFM), Ultrasorb enzyme complex, cocoa, flavour; 5.3 g BCAA, 4 g glutamic acid", "Rich Chocolate"),
        SupplementProduct("Beast Life", "Raw Whey Protein", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 132, 24f, 4f, 2f, "Whey protein concentrate, whey protein isolate, cocoa, digestive enzymes, sucralose", "Chocolate"),
        SupplementProduct("BigMuscles", "Essential Whey", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 118, 24f, 2f, 1.5f, "Whey protein concentrate & whey peptides, cocoa, digestive enzymes, vitamins & minerals, sucralose; no added sugar", "Dutch Chocolate"),
        SupplementProduct("BigMuscles", "Premium Gold Whey", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 136, 25f, 5f, 1.8f, "Whey protein concentrate & isolate blend, ProHydrolase digestive enzyme, cocoa, sucralose; 11 g EAA, zero added sugar", "Belgian Chocolate"),
        SupplementProduct("BSN", "Syntha-6", SupplementCategory.WHEY, "1 scoop (47 g)", 47f, 200, 22f, 15f, 6f, "Sustained-release protein blend (whey concentrate & isolate, casein, milk protein isolate, egg albumen), MCTs, fiber", "Chocolate Milkshake"),
        SupplementProduct("Cellucor", "COR-Performance Whey", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 130, 25f, 3f, 1.5f, "Whey protein isolate & concentrate, cocoa, digestive enzyme blend", "Molten Chocolate"),
        SupplementProduct("Dymatize", "Elite 100% Whey", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 140, 25f, 4f, 3f, "Whey protein concentrate, whey protein isolate, whey peptides (milk), cocoa, natural & artificial flavors, sucralose", "Rich Chocolate"),
        SupplementProduct("Fast&Up", "Whey Protein", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 130, 25f, 3f, 2f, "Whey protein concentrate, whey isolate, cocoa, natural flavour, sucralose", "Chocolate"),
        SupplementProduct("GNC", "AMP Gold 100% Whey Advanced", SupplementCategory.WHEY, "1 scoop (30.5 g)", 30.5f, 120, 24f, 3f, 1.5f, "Whey protein isolate, concentrate & hydrolysate, cocoa, 5.5g BCAA, 4g glutamine, 100mg digestive enzymes", "Double Rich Chocolate"),
        SupplementProduct("GNC", "Pro Performance 100% Whey", SupplementCategory.WHEY, "1 scoop (36 g)", 36f, 130, 24f, 4.5f, 2f, "Whey protein concentrate & isolate blend, cocoa, DigeZyme digestive enzymes, thickeners, sweeteners (sucralose, acesulfame-K); zero added sugar", "Chocolate Fudge"),
        SupplementProduct("HealthKart", "TrueBasics Clean Whey Protein (Isolate + Concentrate)", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 140, 25.2f, 6f, 2f, "Whey protein isolate & concentrate (ultra-filtered), cocoa; no added sugar, sweeteners or fillers", "Chocolate"),
        SupplementProduct("Labrada", "100% Whey Protein", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 125, 25f, 3f, 1.5f, "Whey protein concentrate, whey protein isolate, cocoa, L-carnitine, CLA, natural flavors, lecithin, sucralose", "Chocolate"),
        SupplementProduct("MuscleBlaze", "100% Clean Raw Whey Protein Concentrate", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 118, 24f, 1.8f, 1.5f, "Whey protein concentrate, DigeZyme; unflavoured, no added sugar or maltodextrin", "Unflavoured"),
        SupplementProduct("MuscleBlaze", "Beginner's Whey Protein", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 112, 12f, 13f, 1.5f, "Whey protein concentrate, skimmed milk powder, DigeZyme", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Biozyme Performance Whey", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 140, 25f, 6f, 1.8f, "Whey protein isolate & concentrate blend, creatine monohydrate, DigeZyme, AstraGin, EAF absorption formula", "Rich Chocolate"),
        SupplementProduct("MuscleBlaze", "Fuel One Whey Protein", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 133, 24f, 5f, 2.2f, "Whey protein concentrate & isolate, glutamic acid; no added sugar or maltodextrin", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Raw Whey Protein", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 116, 24f, 1.7f, 1.5f, "100% whey protein concentrate (80%), instantised with soy lecithin", "Unflavoured"),
        SupplementProduct("MusclePharm", "Combat Protein Powder", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 130, 25f, 5f, 2f, "Multi-source protein blend (whey concentrate, isolate, hydrolyzed whey, egg albumin, micellar casein), cocoa, MCTs, flavor, sucralose", "Chocolate Milk"),
        SupplementProduct("MuscleTech", "NitroTech", SupplementCategory.WHEY, "1 scoop (46 g)", 46f, 160, 30f, 4f, 3f, "Whey protein isolate & whey peptides, 3g creatine monohydrate, added BCAAs and glutamine, cocoa", "Milk Chocolate"),
        SupplementProduct("MuscleTech", "NitroTech (Performance Series)", SupplementCategory.WHEY, "1 scoop (46 g)", 46f, 160, 30f, 4f, 2.5f, "Whey protein isolate, whey peptides, whey protein concentrate, creatine monohydrate, cocoa, lecithin, sucralose", "Milk Chocolate"),
        SupplementProduct("MuscleXP", "100% Whey Protein", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 131, 24.9f, 2.5f, 1.5f, "Whey protein concentrate & isolate blend, cocoa, 5.4g BCAA, digestive enzymes", "Double Chocolate"),
        SupplementProduct("MuscleXP", "Premium Whey Protein", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 128, 24f, 3.5f, 2f, "Whey protein concentrate, whey isolate, cocoa, digestive enzymes, sucralose", "Chocolate"),
        SupplementProduct("MyProtein", "Impact Whey Protein", SupplementCategory.WHEY, "1 scoop (25 g)", 25f, 103, 21f, 1f, 1.9f, "Whey protein concentrate (milk, emulsifier: soy lecithin), cocoa, flavoring, sweetener (sucralose)", "Chocolate Smooth (most popular)"),
        SupplementProduct("MyProtein", "THE Whey", SupplementCategory.WHEY, "1 large scoop (30 g)", 30f, 116, 25f, 2f, 1f, "Whey protein blend (isolate, hydrolysate, concentrate; milk), digestive enzymes, flavoring, sweetener (sucralose)", "Salted Caramel"),
        SupplementProduct("Nakpro", "Gold Whey Protein", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 126, 25.5f, 2f, 1.5f, "Whey protein isolate, whey protein concentrate, cocoa, natural flavour, sucralose", "Chocolate"),
        SupplementProduct("Nakpro", "Gold Whey Protein Concentrate", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 140, 28f, 2.4f, 2.1f, "Instantized whey protein concentrate, digestive enzymes; unflavoured no added sugar", "Unflavoured (also Malai Kulfi, Chocolate)"),
        SupplementProduct("Nakpro", "Impact Whey Protein", SupplementCategory.WHEY, "1 scoop (45 g)", 45f, 166, 24f, 14.6f, 1.3f, "Whey protein isolate & concentrate blend, cocoa, digestive enzymes, added vitamins & minerals, sucralose", "Chocolate"),
        SupplementProduct("Nakpro", "Perform Whey Protein Concentrate", SupplementCategory.WHEY, "1 scoop (37 g)", 37f, 146, 24f, 5.5f, 2.5f, "Whey protein concentrate & whey powder blend, cocoa, digestive enzymes, natural flavour, sucralose", "Chocolate"),
        SupplementProduct("Nutrabay", "Gold 100% Whey Protein Concentrate", SupplementCategory.WHEY, "1 scoop (33 g)", 33f, 122, 25f, 2.5f, 1.5f, "Whey protein concentrate, cocoa, digestive enzyme blend (protease, lactase, amylase, lipase, cellulase), sucralose", "Rich Chocolate Creme"),
        SupplementProduct("Nutrabay", "Pure Whey Protein Concentrate", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 120, 24f, 2f, 1.8f, "100% raw whey protein concentrate; no additives, unflavoured", "Unflavoured"),
        SupplementProduct("Optimum Nutrition", "Gold Standard 100% Whey", SupplementCategory.WHEY, "1 scoop (30.4 g)", 30.4f, 120, 24f, 3f, 1.5f, "Whey protein isolate, whey protein concentrate, whey peptides, cocoa, natural & artificial flavor, lecithin, sucralose, acesulfame potassium", "Double Rich Chocolate"),
        SupplementProduct("Optimum Nutrition", "Performance Whey", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 123, 24f, 3f, 1.5f, "Whey protein concentrate & isolate, DigeZyme digestive enzyme blend, cocoa, natural & artificial flavor, lecithin, sucralose", "Chocolate Milkshake"),
        SupplementProduct("OZiva", "Protein & Herbs for Men", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 118, 23f, 3f, 1.5f, "Whey protein isolate & concentrate, ayurvedic herbs (ashwagandha, safed musli, mucuna), multivitamins, cocoa; no added sugar, soy-free", "Chocolate"),
        SupplementProduct("OZiva", "Protein & Herbs for Women", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 125, 23f, 3f, 2f, "Whey protein isolate & concentrate, curcumin, shatavari, tulsi, multivitamins, cocoa; no added sugar", "Chocolate"),
        SupplementProduct("Proathlix", "Universal Blend Whey Protein", SupplementCategory.WHEY, "1 scoop (36 g)", 36f, 125, 24f, 4f, 1.5f, "9-source whey blend with veg collagen peptide, DigeZyme enzyme complex, cocoa, natural flavour, sucralose", "Chocolate"),
        SupplementProduct("Proathlix", "Xloaded Whey Protein", SupplementCategory.WHEY, "1 scoop (31 g)", 31f, 112, 25f, 1.5f, 0.5f, "Whey protein concentrate/isolate blend, cocoa, natural flavour, sucralose; 4.7 g BCAA, glutamic acid", "Chocolate"),
        SupplementProduct("Rule One", "R1 Whey Blend", SupplementCategory.WHEY, "1 scoop (34 g)", 34f, 150, 24f, 6f, 3f, "Whey protein concentrate & isolate blend, cocoa, natural & artificial flavor, sunflower lecithin, sucralose", "Chocolate Fudge"),
        SupplementProduct("Scitec Nutrition", "100% Whey Protein Professional", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 112, 22f, 1.4f, 2f, "Whey protein concentrate & isolate, added L-glutamine and L-leucine, digestive enzyme complex", "Chocolate"),
        SupplementProduct("Scitron", "Advance Whey Protein", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 132, 25.5f, 4f, 1.5f, "Hydrolyzed & micro-filtered whey isolate/concentrate blend (Glanbia), 20 vitamins & minerals, cocoa, sucralose; 0 g sugar", "Milk Chocolate"),
        SupplementProduct("Scitron", "Raw Whey", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 120, 24f, 1.4f, 1.5f, "100% whey protein concentrate, unflavoured, no added sugar or sweetener", "Unflavoured"),
        SupplementProduct("TrueBasics", "100% Whey Protein", SupplementCategory.WHEY, "1 scoop (35 g)", 35f, 137, 25.5f, 4f, 2f, "Whey protein isolate, whey concentrate, DigeZyme, cocoa, sucralose", "Chocolate"),
        SupplementProduct("Ultimate Nutrition", "Prostar 100% Whey Protein", SupplementCategory.WHEY, "1 scoop (30 g)", 30f, 120, 25f, 2f, 1f, "Whey protein isolate, whey protein concentrate, whey peptides, cocoa, natural flavors, lecithin, sucralose", "Chocolate Creme"),
        // ── ISOLATE ──
        SupplementProduct("Applied Nutrition", "ISO-XP", SupplementCategory.ISOLATE, "1 scoop (25 g)", 25f, 90, 22.5f, 0.1f, 0.2f, "100% whey protein isolate, flavoring, emulsifier (sunflower lecithin), sucralose; ultra-low lactose", "Vanilla"),
        SupplementProduct("AS-IT-IS", "ATOM Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (33 g)", 33f, 124, 29f, 1f, 0.5f, "Whey protein isolate, DigeZyme digestive enzymes, cocoa, natural flavour, sweetener (sucralose); near-zero lactose", "Rich Chocolate"),
        SupplementProduct("AS-IT-IS", "Raw Whey Protein Isolate 90%", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 114, 27f, 0.5f, 0.3f, "Whey protein isolate (unflavoured), cross-flow microfiltered; no added sugar, no artificial sweeteners", "Unflavoured"),
        SupplementProduct("Avvatar", "Isorich Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 126, 28f, 1f, 1.2f, "Whey protein isolate (from cow's milk), natural flavour, sweetener (sucralose); low lactose, no added sugar", "Belgian Chocolate"),
        SupplementProduct("Beast Life", "Isorich Blend Whey Protein", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 115, 24f, 3f, 1.5f, "Whey protein isolate + whey protein concentrate blend, Ultrasorb enzymes, cocoa, sucralose; ~5.5 g BCAA, ~2.5 g leucine", "Malai Kulfi"),
        SupplementProduct("Beast Life", "Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 115, 26f, 1f, 0.5f, "Whey protein isolate, cocoa, natural flavour, digestive enzymes, sucralose", "Chocolate"),
        SupplementProduct("BigMuscles", "Nitric Whey", SupplementCategory.ISOLATE, "1 scoop (35 g)", 35f, 130, 27f, 6f, 1.5f, "Whey protein isolate/concentrate, L-Arginine, added flavour, calcium phosphate, sucralose; 5.5 g BCAA, 4.4 g glutamine", "Kiwi Strawberry"),
        SupplementProduct("BSN", "Syntha-6 Isolate", SupplementCategory.ISOLATE, "1 scoop (34 g)", 34f, 150, 25f, 7f, 2f, "Whey protein isolate and milk protein isolate blend, 11g EAAs, cocoa", "Chocolate Milkshake"),
        SupplementProduct("Dymatize", "ISO100 Clear", SupplementCategory.ISOLATE, "1 scoop (25 g)", 25f, 90, 20f, 1f, 0f, "Hydrolyzed whey protein isolate (milk), citric acid, natural flavors, sucralose", "Orange Mango"),
        SupplementProduct("Dymatize", "ISO100 Hydrolyzed", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 120, 25f, 2f, 1f, "Hydrolyzed whey protein isolate, whey protein isolate (milk), cocoa, flavors, sucralose", "Gourmet Chocolate"),
        SupplementProduct("Fast&Up", "Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (31 g)", 31f, 110, 26f, 0.5f, 0.5f, "90% European whey protein isolate, cocoa, 6g BCAA, 4.5g glutamine; ultra-low carb, low lactose, gluten-free", "Rich Chocolate"),
        SupplementProduct("GNC", "AMP Pure Isolate", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 125, 25f, 4f, 1f, "Micro-filtered whey protein isolate, cocoa, 6g BCAA, digestive enzymes; lactose-free, zero added sugar", "Chocolate"),
        SupplementProduct("HealthKart", "TrueBasics Clean Whey 100% Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (34 g)", 34f, 130, 30f, 1f, 0.8f, "100% whey protein isolate; no added sugar, no artificial flavours or fillers", "Vanilla"),
        SupplementProduct("Isopure", "Isopure Infusions Clear Whey Isolate", SupplementCategory.ISOLATE, "1 scoop (23 g)", 23f, 90, 20f, 1f, 0f, "Whey protein isolate, natural flavor, citric acid, stevia leaf extract", "Tropical Punch"),
        SupplementProduct("Isopure", "Low Carb 100% Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 130, 25f, 3f, 1.5f, "Whey protein isolate, vitamin & mineral blend, cocoa, natural flavors, soy lecithin, sucralose", "Dutch Chocolate"),
        SupplementProduct("Isopure", "Whey Protein Isolate Unflavored", SupplementCategory.ISOLATE, "1 scoop (29 g)", 29f, 100, 25f, 0f, 0f, "100% whey protein isolate, vitamin & mineral blend, soy lecithin", "Unflavored"),
        SupplementProduct("Isopure", "Zero Carb 100% Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (31 g)", 31f, 110, 25f, 0f, 0.5f, "Whey protein isolate, vitamin & mineral blend, natural flavors, salt, soy lecithin, sucralose", "Creamy Vanilla"),
        SupplementProduct("Kaged", "Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (34 g)", 34f, 130, 25f, 3f, 1.5f, "Whey protein isolate, cocoa, natural flavors, sunflower lecithin, stevia, sucralose", "Chocolate"),
        SupplementProduct("Kaged", "Whey Protein Isolate (MicroPure)", SupplementCategory.ISOLATE, "1 scoop (31 g)", 31f, 110, 25f, 3f, 0.5f, "Ultra-filtered whey protein isolate, ProHydrolase digestive enzymes, natural flavor, sunflower lecithin, stevia; 5.5g BCAAs", "Vanilla"),
        SupplementProduct("Labrada", "ISO LeanPro 100% Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 115, 25f, 1f, 1f, "100% whey protein isolate, cocoa, natural flavors, lecithin, sucralose", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Biozyme Iso-Zero", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 115, 27f, 1f, 0.5f, "Whey protein isolate, EAF enhanced absorption formula, DigeZyme, sucralose", "Low Carb Ice Cream Chocolate"),
        SupplementProduct("MuscleBlaze", "Biozyme Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (31 g)", 31f, 111, 25f, 1.9f, 0.4f, "Whey protein isolate, EAF enhanced absorption formula, DigeZyme, sucralose", "Ice Cream Chocolate"),
        SupplementProduct("MuscleBlaze", "Whey Gold", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 110, 25f, 1.6f, 0.5f, "Whey protein isolate (primary), whey protein concentrate, DigeZyme", "Rich Milk Chocolate"),
        SupplementProduct("MuscleTech", "Nitro-Tech ISO Whey", SupplementCategory.ISOLATE, "1 scoop (31 g)", 31f, 110, 25f, 1f, 0.5f, "Pure whey protein isolate and whey peptides, 11g EAAs, 5g BCAAs, cocoa", "Milk Chocolate"),
        SupplementProduct("MuscleTech", "NitroTech 100% ISO Whey", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 110, 25f, 1f, 0.5f, "Whey protein isolate, whey peptides, natural & artificial flavors, sucralose", "Vanilla"),
        SupplementProduct("MuscleXP", "Original Whey Isolate 90%", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 116, 27f, 1f, 0.5f, "90% whey protein isolate (imported), 7.28g BCAA, 3.9g L-glutamic acid, digestive enzymes; unflavoured", "Unflavoured"),
        SupplementProduct("MyProtein", "Clear Whey Isolate", SupplementCategory.ISOLATE, "1 scoop (25 g)", 25f, 87, 20f, 0.5f, 0.1f, "Hydrolyzed whey protein isolate (milk), acidity regulators, natural flavoring, sweetener (sucralose)", "Lemonade"),
        SupplementProduct("MyProtein", "Impact Whey Isolate", SupplementCategory.ISOLATE, "1 scoop (25 g)", 25f, 93, 22f, 0.6f, 0.1f, "Whey protein isolate (milk, emulsifier: soy lecithin), flavoring, sweetener (sucralose)", "Vanilla"),
        SupplementProduct("Nakpro", "Platinum Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (34 g)", 34f, 130, 28f, 2f, 0.5f, "Whey protein isolate, multivitamin & mineral blend, digestive enzymes, natural flavour, sucralose", "Chocolate"),
        SupplementProduct("Nutrabay", "Gold Tri-Blend Whey Protein", SupplementCategory.ISOLATE, "3/4 scoop (33 g)", 33f, 121, 25f, 2f, 1.5f, "Whey protein concentrate, whey isolate, hydrolysed whey, cocoa, digestive enzymes, sucralose", "Rich Chocolate Creme"),
        SupplementProduct("Nutrabay", "Pure 100% Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 113, 26.5f, 1f, 0.5f, "100% cross-flow microfiltered whey protein isolate; no additives, unflavoured", "Unflavoured"),
        SupplementProduct("Optimum Nutrition", "Gold Standard 100% Isolate", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 110, 25f, 1f, 0.5f, "Hydrolyzed & ultra-filtered whey protein isolate, cocoa, natural & artificial flavor, lecithin, sucralose, acesulfame potassium", "Chocolate Bliss"),
        SupplementProduct("Optimum Nutrition", "Platinum Hydrowhey", SupplementCategory.ISOLATE, "1 scoop (39 g)", 39f, 140, 30f, 3f, 1f, "Hydrolyzed whey protein isolate, added BCAAs (leucine, isoleucine, valine), cocoa, natural & artificial flavor, lecithin, sucralose", "Turbo Chocolate"),
        SupplementProduct("Proathlix", "Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 112, 25f, 0.4f, 0.5f, "Whey protein isolate, alkalized cocoa, natural flavour, sodium chloride, sucralose; 5.5 g BCAA", "Chocolate"),
        SupplementProduct("Rule One", "R1 Protein (Whey Isolate)", SupplementCategory.ISOLATE, "1 scoop (31 g)", 31f, 110, 25f, 1f, 0.5f, "Whey protein isolate & hydrolysate, sunflower lecithin, natural & artificial flavor, sucralose; 6g BCAAs", "Vanilla Creme"),
        SupplementProduct("Scitec Nutrition", "100% Isolate", SupplementCategory.ISOLATE, "1 scoop (25 g)", 25f, 93, 21f, 1.4f, 0.5f, "Whey protein isolate only (no concentrate), >4g BCAA and added glutamine per serving", "Chocolate"),
        SupplementProduct("Scitron", "Raw ISO Whey Protein Isolate", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 113, 27f, 0.5f, 0.3f, "100% whey protein isolate, unflavoured, no added sugar or sweetener", "Unflavoured"),
        SupplementProduct("Ultimate Nutrition", "ISO Sensation 93", SupplementCategory.ISOLATE, "1 scoop (32 g)", 32f, 130, 30f, 1f, 1f, "Whey protein isolate (CFM), lactase & protease enzymes, cocoa, natural flavors, lecithin, sucralose", "Chocolate Fudge"),
        SupplementProduct("Wellcore", "100% Whey Isolate", SupplementCategory.ISOLATE, "1 scoop (30 g)", 30f, 118, 27f, 1f, 0.5f, "Whey protein isolate (micro/ultra-filtered), cocoa, natural & artificial flavour, sucralose; 5.75 g BCAA", "Chocolate"),
        // ── CASEIN ──
        SupplementProduct("Dymatize", "Elite Casein", SupplementCategory.CASEIN, "2 scoops (36 g)", 36f, 130, 25f, 3f, 2f, "100% micellar casein (milk), sunflower creamer, cocoa, natural & artificial flavors, sucralose", "Rich Chocolate"),
        SupplementProduct("MuscleBlaze", "100% Micellar Casein", SupplementCategory.CASEIN, "1 scoop (35 g)", 35f, 125, 24f, 5.7f, 0.8f, "Micellar casein (slow-release), DigeZyme, cocoa powder", "Chocolate"),
        SupplementProduct("MyProtein", "Slow-Release Casein", SupplementCategory.CASEIN, "1 scoop (30 g)", 30f, 110, 24f, 2f, 1f, "Micellar casein (milk), flavoring, sweetener (sucralose)", "Chocolate"),
        SupplementProduct("Optimum Nutrition", "Gold Standard 100% Casein", SupplementCategory.CASEIN, "1 scoop (33 g)", 33f, 120, 24f, 3f, 1f, "Micellar casein, cocoa, natural & artificial flavor, lecithin, cellulose gum, sucralose, acesulfame potassium", "Creamy Vanilla"),
        SupplementProduct("Rule One", "R1 Casein", SupplementCategory.CASEIN, "1 scoop (36 g)", 36f, 120, 25f, 2f, 1f, "100% micellar casein, cocoa, natural & artificial flavor, sunflower lecithin, sucralose", "Chocolate Fudge"),
        // ── PLANT ──
        SupplementProduct("Amway", "Nutrilite All Plant Protein", SupplementCategory.PLANT, "3 tbsp (10 g)", 10f, 40, 8f, 1f, 0.3f, "Soy protein isolate, wheat protein, yellow pea protein", "Unflavoured"),
        SupplementProduct("AS-IT-IS", "Pea Protein Isolate", SupplementCategory.PLANT, "1 scoop (30 g)", 30f, 118, 24f, 1.5f, 1.5f, "Pea protein isolate (unflavoured), plant-based, vegan, gluten-free; ~2 g dietary fiber", "Unflavoured"),
        SupplementProduct("Beast Life", "Performance Fermented Yeast Protein", SupplementCategory.PLANT, "1 scoop (30 g)", 30f, 120, 25f, 2f, 1.5f, "Fermented yeast protein, beta-glucan, Ultrasorb tech, cocoa, flavour; 5.64 g BCAA, easy-digest & gut friendly, dairy-free", "Chocolate"),
        SupplementProduct("bGREEN", "Plant Protein", SupplementCategory.PLANT, "1 scoop (37 g)", 37f, 140, 25f, 3f, 3.5f, "Pea protein isolate, brown rice protein, DigeZyme, cocoa, stevia", "Chocolate"),
        SupplementProduct("Fast&Up", "Plant Protein", SupplementCategory.PLANT, "1 scoop (35 g)", 35f, 140, 26f, 4f, 2f, "Pea protein isolate & brown rice protein, 4.6g BCAA, 4.8g glutamine, digestive enzymes (Pepzyme Pro) & probiotics; no added sugar", "Rich Chocolate"),
        SupplementProduct("Garden of Life", "Raw Organic Protein", SupplementCategory.PLANT, "1 scoop (33 g)", 33f, 130, 22f, 6f, 2.5f, "Organic pea protein, sprouted brown rice, amaranth/buckwheat/quinoa sprouts, chlorella, probiotic & enzyme blend", "Vanilla"),
        SupplementProduct("Garden of Life", "Sport Organic Plant Protein", SupplementCategory.PLANT, "1 scoop (43 g)", 43f, 170, 30f, 6f, 3f, "Organic pea protein, organic navy bean, lentil, garbanzo, probiotics", "Chocolate"),
        SupplementProduct("Garden of Life", "Sport Organic Plant-Based Protein", SupplementCategory.PLANT, "1 scoop (42 g)", 42f, 170, 30f, 5f, 3f, "Organic pea protein, sprouted navy/lentil/garbanzo bean protein, organic cranberry protein, BCAAs, tart cherry, probiotics", "Vanilla"),
        SupplementProduct("GNC", "AMP Plant Isolate", SupplementCategory.PLANT, "1 scoop (34.5 g)", 34.5f, 130, 25f, 3f, 2f, "Pea protein isolate & brown rice protein, 10g EAA, DigeZyme enzymes, added vitamins/minerals & electrolytes; vegan, soy-free, lactose-free", "Chocolate Hazelnut"),
        SupplementProduct("Kaged", "Plantein", SupplementCategory.PLANT, "1 scoop (35 g)", 35f, 150, 25f, 3f, 4f, "ioPea pea protein isolate, natural flavor, sea salt, sunflower lecithin, stevia; all 9 EAAs, 4.5g BCAAs", "Chocolate"),
        SupplementProduct("Kapiva", "Plant Protein", SupplementCategory.PLANT, "1 scoop (35 g)", 35f, 125, 24.5f, 4f, 1.5f, "Pea protein isolate, rice protein isolate, 26 vitamins & minerals; 4.97g BCAA, 0g added sugar", "Chocolate"),
        SupplementProduct("MuscleBlaze", "bGREEN Plant Protein", SupplementCategory.PLANT, "1 scoop (36 g)", 36f, 140, 22f, 7.3f, 2.8f, "Pea protein isolate, brown rice protein, DigeZyme; vegan, no soy/gluten/lactose", "Chocolate"),
        SupplementProduct("MyProtein", "Vegan Protein Blend", SupplementCategory.PLANT, "1 scoop (30 g)", 30f, 110, 21f, 2f, 2.5f, "Pea protein isolate, brown rice protein, flavoring, sweetener (sucralose)", "Chocolate"),
        SupplementProduct("Nutrabay", "Wellness Vegan Plant Protein + Superfoods", SupplementCategory.PLANT, "1 scoop (34 g)", 34f, 130, 24f, 4f, 2f, "Pea protein isolate, brown rice protein, superfoods (spirulina, flaxseed, moringa), 13 vitamins & 11 minerals, digestive enzymes, stevia", "Rich Chocolate"),
        SupplementProduct("Optimum Nutrition", "Gold Standard 100% Plant", SupplementCategory.PLANT, "1 scoop (36 g)", 36f, 150, 24f, 6f, 3f, "Pea protein, rice protein, fava bean protein, cocoa, natural flavor, sea salt, stevia, no artificial colors/flavors/sweeteners", "Rich Chocolate"),
        SupplementProduct("Origin Nutrition", "100% Vegan Plant Protein", SupplementCategory.PLANT, "1 scoop (37 g)", 37f, 140, 25f, 3f, 3f, "Pea protein isolate, brown rice protein, natural flavour, stevia", "Rich Chocolate"),
        SupplementProduct("Origin Nutrition", "Daily Plant Protein", SupplementCategory.PLANT, "1 scoop (33 g)", 33f, 130, 25f, 4f, 1.5f, "European golden pea protein isolate, cold-pressed pumpkin seed protein, digestive enzymes, natural cocoa; 4.5g BCAA, no added sugar", "Chocolate Fudge"),
        SupplementProduct("OZiva", "Organic Plant Protein", SupplementCategory.PLANT, "1 scoop (36 g)", 36f, 140, 30f, 3f, 1.5f, "Organic pea protein isolate, organic brown rice protein, quinoa; 5.2g BCAA, sugar-free, lactose-free, non-GMO", "Unflavoured"),
        SupplementProduct("Plix", "Evolve Performance Plant Protein", SupplementCategory.PLANT, "1 scoop (34 g)", 34f, 112, 25f, 2f, 1f, "Pea protein isolate, brown rice protein, turmeric extract, acai berry extract, digestive enzymes; stevia-sweetened, no added sugar", "Vanilla"),
        SupplementProduct("Plix", "Plant Protein", SupplementCategory.PLANT, "1 scoop (30 g)", 30f, 110, 20f, 3f, 2f, "Pea protein isolate, natural flavour, stevia", "Chocolate"),
        SupplementProduct("Plix", "Strength Plant Protein", SupplementCategory.PLANT, "1 scoop (34 g)", 34f, 108, 25f, 1.5f, 0.6f, "Pea protein isolate, brown rice protein, grape seed extract, lycopene, digestive enzymes (bromelain, papain), Bacillus coagulans probiotic; no added sugar", "Raw Chocolate"),
        SupplementProduct("Vega", "Protein & Greens", SupplementCategory.PLANT, "1 scoop (33 g)", 33f, 110, 20f, 4f, 2f, "Pea protein, brown rice protein, sacha inchi protein, alfalfa, spinach/broccoli/kale greens", "Vanilla"),
        SupplementProduct("Vega", "Sport Premium Protein", SupplementCategory.PLANT, "1 scoop (44 g)", 44f, 160, 30f, 4f, 3f, "Pea protein, pumpkin seed protein, sunflower seed protein, alfalfa protein, tart cherry, BCAAs, probiotics", "Vanilla"),
        SupplementProduct("Wellbeing Nutrition", "Superfood Plant Protein", SupplementCategory.PLANT, "1 scoop (32 g)", 32f, 125, 22f, 5f, 2.5f, "Golden pea protein isolate, brown rice protein, chia seed protein, spirulina, moringa, chlorella, DigeZyme enzymes; 5g BCAA, 3g fibre, stevia", "Belgian Dark Chocolate"),
        // ── GAINER ──
        SupplementProduct("Applied Nutrition", "Critical Mass", SupplementCategory.GAINER, "4 scoops (240 g)", 240f, 917, 54.9f, 153.8f, 9f, "Carb blend (maltodextrin, oat flour), whey protein concentrate, creatine, HMB, MCT, flavoring, sweetener", "Chocolate"),
        SupplementProduct("Applied Nutrition", "Critical Mass (Original)", SupplementCategory.GAINER, "4 scoops (240 g)", 240f, 917, 54.9f, 153.8f, 9f, "Carb blend (maltodextrin, oat flour), whey protein concentrate, creatine, HMB, MCT, flavoring, sweetener", "Chocolate"),
        SupplementProduct("AS-IT-IS", "ATOM Mass Gainer", SupplementCategory.GAINER, "1 serving (75 g)", 75f, 315, 12f, 60f, 3f, "Maltodextrin, whey protein concentrate, BCAA, L-glutamine, tribulus, ashwagandha, cocoa; 5:1 carb-protein ratio", "Double Rich Chocolate"),
        SupplementProduct("Avvatar", "Mass Gainer", SupplementCategory.GAINER, "1 scoop (60 g)", 60f, 218, 12.8f, 38f, 1.5f, "Maltodextrin, whey protein concentrate & isolate, cocoa, 21 vitamins & minerals, natural flavour; no added sugar", "Belgian Chocolate"),
        SupplementProduct("Avvatar", "Muscle Gainer", SupplementCategory.GAINER, "1 scoop (60 g)", 60f, 218, 25.5f, 24f, 2f, "Whey protein concentrate & isolate, casein, maltodextrin, dextrose, 21 vitamins & minerals; no added sugar", "Belgian Chocolate"),
        SupplementProduct("Beast Life", "Anabolic Mass Gainer", SupplementCategory.GAINER, "3 scoops (120 g)", 120f, 465, 22f, 92f, 3f, "Whey concentrate & milk protein blend, complex carbs from oats/wheat/barley, creatine 3 g, MCT oil, digestive enzymes, B-vitamins", "Belgian Chocolate"),
        SupplementProduct("BigMuscles", "Real Mass Gainer", SupplementCategory.GAINER, "1 scoop (75 g)", 75f, 284, 17f, 53f, 2.5f, "Maltodextrin, protein blend (skimmed milk powder, soya isolate, WPC, milk protein concentrate, WPI), sucrose, cocoa, BCAA, L-glutamine", "Chocolate"),
        SupplementProduct("BigMuscles", "Xtreme Mass Gainer", SupplementCategory.GAINER, "3 scoops (100 g)", 100f, 404, 18f, 77f, 3f, "Maltodextrin, skimmed & whole milk powder, soya protein isolate, WPC, sucrose, creatine monohydrate, cocoa, calcium phosphate", "Chocolate"),
        SupplementProduct("BSN", "True-Mass", SupplementCategory.GAINER, "2 scoops (165 g)", 165f, 700, 50f, 90f, 17f, "Protein blend (whey concentrate & isolate, casein, milk protein), complex carbs, MCTs, 6g fiber", "Chocolate Milkshake"),
        SupplementProduct("Dymatize", "Super Mass Gainer", SupplementCategory.GAINER, "2.5 cups (334 g)", 334f, 1280, 52f, 245f, 9f, "Maltodextrin, protein blend (whey concentrate, isolate, casein; milk), creatine, cocoa, vitamins & minerals, flavors", "Rich Chocolate"),
        SupplementProduct("GNC", "Pro Performance Weight Gainer", SupplementCategory.GAINER, "3 scoops (~190 g) in water", 190f, 700, 73f, 115f, 6f, "Whey protein blend, complex carbohydrates (maltodextrin/oats), cocoa, dietary fibre, digestive enzymes, added vitamins & minerals", "Double Chocolate"),
        SupplementProduct("Labrada", "Muscle Mass Gainer", SupplementCategory.GAINER, "7 scoops (333 g)", 333f, 1284, 52f, 250f, 8f, "Maltodextrin, whey protein concentrate, whey protein isolate, calcium caseinate, creatine monohydrate, L-glutamine, cocoa", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Mass Gainer Pro", SupplementCategory.GAINER, "2 scoops (150 g)", 150f, 543, 30f, 92f, 6.5f, "Complex carbs, whey concentrate, milk protein, oats, flaxseed, digestive enzymes", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Mass Gainer Pro with Creapure", SupplementCategory.GAINER, "1 scoop (75 g)", 75f, 305, 18f, 54f, 2f, "Complex carbs, whey protein, Creapure creatine, betaine, L-glutamine, MCT oil, 21 vitamins & minerals", "Chocolate"),
        SupplementProduct("MuscleBlaze", "Super Gainer XXL", SupplementCategory.GAINER, "1 scoop (100 g)", 100f, 377, 15f, 75f, 2f, "Maltodextrin, whey protein concentrate, skimmed milk powder, 27 vitamins & minerals, DigeZyme", "Chocolate"),
        SupplementProduct("MusclePharm", "Combat XL Mass Gainer", SupplementCategory.GAINER, "4 scoops (332 g)", 332f, 1270, 50f, 252f, 7f, "Carb blend (maltodextrin), protein blend (whey concentrate, casein), MCTs, creatine, glutamine, cocoa", "Chocolate"),
        SupplementProduct("MuscleTech", "Mass-Tech Extreme 2000", SupplementCategory.GAINER, "3 scoops (285 g)", 285f, 1030, 30f, 222f, 2.5f, "Multi-stage carb complex (maltodextrin), whey protein blend, L-leucine, creatine monohydrate", "Triple Chocolate Brownie"),
        SupplementProduct("MuscleXP", "Pro Mass Gainer", SupplementCategory.GAINER, "1 scoop (75 g) in water", 75f, 304, 27f, 39f, 2.5f, "Whey protein concentrate 80% & isolate, calcium caseinate, skimmed & whole milk powder, maltodextrin, 25 vitamins & minerals, digestive enzymes", "Double Chocolate"),
        SupplementProduct("MyProtein", "Weight Gainer Blend", SupplementCategory.GAINER, "1 serving (100 g)", 100f, 388, 31f, 50f, 6.2f, "Whey protein concentrate (milk), maltodextrin, oat flour, ground almonds, flavoring", "Chocolate Smooth"),
        SupplementProduct("Nakpro", "Gold Mass Gainer", SupplementCategory.GAINER, "1 scoop (100 g)", 100f, 372, 21.6f, 68.6f, 1.5f, "Maltodextrin, whey protein concentrate, cocoa, added vitamins & minerals, digestive enzymes", "Chocolate"),
        SupplementProduct("Nutrabay", "Gold Mega Mass Gainer", SupplementCategory.GAINER, "2 scoops (175 g)", 175f, 711, 40.7f, 122f, 7f, "Maltodextrin, whey protein concentrate, oat/complex carbs, cocoa, digestive enzymes, vitamins & minerals", "Chocolate"),
        SupplementProduct("Optimum Nutrition", "Serious Mass", SupplementCategory.GAINER, "2 heaping scoops (334 g)", 334f, 1250, 50f, 252f, 4.5f, "Maltodextrin, whey protein concentrate, 5g creatine monohydrate, 3g glutamine, 19 vitamins & minerals, cocoa, lecithin", "Chocolate"),
        SupplementProduct("Rule One", "R1 Mass Gainer", SupplementCategory.GAINER, "2 heaping scoops (318 g)", 318f, 1220, 40f, 250f, 7f, "Complex & simple carb blend (maltodextrin), all-whey protein, creatine monohydrate, MCTs, glutamine, cocoa; 9g BCAAs", "Chocolate Fudge"),
        SupplementProduct("Scitec Nutrition", "Jumbo", SupplementCategory.GAINER, "5 scoops (220 g)", 220f, 849, 53f, 140f, 7.4f, "Multi-component protein (whey, milk, egg, soy), carbohydrate complex, 3g creatine, amino matrix, vitamins, plant extracts", "Vanilla"),
        SupplementProduct("Scitron", "Mega Mass Gainer", SupplementCategory.GAINER, "4 scoops (150 g)", 150f, 562, 22.5f, 112.5f, 2.5f, "Maltodextrin, whey protein concentrate, creatine monohydrate 3 g, L-glutamine, 20 vitamins & minerals, cocoa", "Rich Chocolate"),
        SupplementProduct("Ultimate Nutrition", "Muscle Juice Revolution 2600", SupplementCategory.GAINER, "4 scoops (265 g)", 265f, 1020, 56f, 170f, 13f, "Maltodextrin, Octo-PRO protein blend (whey concentrate, isolate, micellar casein, egg albumin), MCT & flax oil, glutamine, cocoa", "Chocolate"),
        // ── CREATINE ──
        SupplementProduct("Applied Nutrition", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 serving (5 g)", 5f, 0, 0f, 0f, 0f, "100% pure micronized creatine monohydrate, no fillers or sweeteners; Informed-Sport tested", "Unflavoured"),
        SupplementProduct("AS-IT-IS", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "100% pure creatine monohydrate; no additives, no preservatives", "Unflavoured"),
        SupplementProduct("Avvatar", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate; no fillers, no added sugar", "Unflavoured"),
        SupplementProduct("Beast Life", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (5 g)", 5f, 0, 0f, 0f, 0f, "Super micronized creatine monohydrate for high absorption, unflavoured", "Unflavoured"),
        SupplementProduct("BigMuscles", "Benchmark Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate, unflavoured", "Unflavoured"),
        SupplementProduct("Dymatize", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (5 g)", 5f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate (Creapure), no fillers or additives", "Unflavored"),
        SupplementProduct("Fast&Up", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "100% pure ultra-micronized creatine monohydrate (3g per serve); sugar-free, vegetarian", "Unflavoured"),
        SupplementProduct("GNC", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (5 g)", 5f, 0, 0f, 0f, 0f, "Creatine monohydrate", "Unflavoured"),
        SupplementProduct("GNC", "Pro Performance Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate (3g per serve); sugar-free, gluten-free, vegetarian", "Unflavoured"),
        SupplementProduct("Kaged", "C-HCl Creatine HCl", SupplementCategory.CREATINE, "750 mg (1 capsule)", 0.8f, 0, 0f, 0f, 0f, "Patented creatine hydrochloride (C-HCl), vegetable cellulose capsule; no fillers or dyes", "Unflavored"),
        SupplementProduct("MuscleBlaze", "Creatine Monohydrate CreAMP", SupplementCategory.CREATINE, "1 scoop (3.4 g)", 3.4f, 0, 0f, 0f, 0f, "Micronised creatine monohydrate 3 g, MB CreAbsorb blend", "Fruit Punch"),
        SupplementProduct("MusclePharm", "Creatine", SupplementCategory.CREATINE, "1 scoop (5 g)", 5f, 0, 0f, 0f, 0f, "Creatine matrix: creatine monohydrate, creatine HCl, creatine AAKG, creatine malate, magnesium creatine chelate", "Unflavored"),
        SupplementProduct("MuscleTech", "Cell-Tech", SupplementCategory.CREATINE, "1 scoop (44 g)", 44f, 150, 0f, 38f, 0f, "5g creatine (monohydrate + citrate), multi-stage carbs (maltodextrin, dextrose, ModCarb), taurine, alpha-lipoic acid", "Fruit Punch"),
        SupplementProduct("MuscleTech", "Platinum 100% Creatine", SupplementCategory.CREATINE, "1 scoop (5 g)", 5f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate (5g), HPLC-tested, no fillers", "Unflavored"),
        SupplementProduct("MuscleXP", "Micronized Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate (3g per serve); unflavoured, sugar-free, vegetarian", "Unflavoured"),
        SupplementProduct("MyProtein", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "Creatine monohydrate (100%)", "Unflavored"),
        SupplementProduct("Nakpro", "Micronized Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate (3 g creatine per serve)", "Unflavoured"),
        SupplementProduct("Nutrabay", "Pure Creatine Monohydrate Micronized", SupplementCategory.CREATINE, "1 scoop (3.4 g)", 3.4f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate; no fillers or additives", "Unflavoured"),
        SupplementProduct("Optimum Nutrition", "Micronized Creatine Powder", SupplementCategory.CREATINE, "1 rounded tsp (5 g)", 5f, 0, 0f, 0f, 0f, "100% Creapure micronized creatine monohydrate, unflavored", "Unflavored"),
        SupplementProduct("Rule One", "R1 Creatine", SupplementCategory.CREATINE, "1 scoop (5 g)", 5f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate, no fillers or additives", "Unflavored"),
        SupplementProduct("Scitron", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "100% creatine monohydrate (3 g per serve), 0 fillers, no flavour or sweetener", "Unflavoured"),
        SupplementProduct("Ultimate Nutrition", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 rounded tsp (5 g)", 5f, 0, 0f, 0f, 0f, "100% micronized creatine monohydrate", "Unflavored"),
        SupplementProduct("Wellcore", "Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "Micronized creatine monohydrate", "Unflavoured"),
        SupplementProduct("Wellcore", "Micronised Creatine Monohydrate", SupplementCategory.CREATINE, "1 scoop (3 g)", 3f, 0, 0f, 0f, 0f, "100% pure micronised creatine monohydrate; no fillers, flavours or added sugar", "Unflavoured"),
        // ── BCAA_EAA ──
        SupplementProduct("Beast Life", "EAA", SupplementCategory.BCAA_EAA, "1 scoop (10 g)", 10f, 5, 0f, 1f, 0f, "Essential amino acids incl. BCAAs (2:1:1), electrolytes, flavouring, sucralose", "Watermelon"),
        SupplementProduct("BigMuscles", "Real BCAA", SupplementCategory.BCAA_EAA, "1 scoop (5 g)", 5f, 5, 0f, 0.5f, 0f, "Micronized L-Leucine, L-Isoleucine, L-Valine (2:1:1), sodium citrate, potassium chloride electrolytes, sucralose, flavour", "Green Apple"),
        SupplementProduct("BSN", "Amino X", SupplementCategory.BCAA_EAA, "1 scoop (14.5 g)", 14.5f, 5, 0f, 1f, 0f, "10g BCAA blend (2:1:1 leucine/isoleucine/valine), L-alanine, taurine, L-citrulline, vitamin D, stimulant-free", "Blue Raspberry"),
        SupplementProduct("Cellucor", "Alpha Amino", SupplementCategory.BCAA_EAA, "1 scoop (13 g)", 13f, 0, 0f, 0f, 0f, "5g BCAAs (2:1:1), EAA performance blend, taurine, coconut water powder, BetaPower betaine, electrolytes", "Icy Blue Razz"),
        SupplementProduct("Fast&Up", "BCAA", SupplementCategory.BCAA_EAA, "1 scoop (7.5 g)", 7.5f, 20, 5f, 0f, 0f, "5g BCAA 2:1:1 (leucine, isoleucine, valine), L-glutamine, L-arginine, L-citrulline, taurine, electrolytes & vitamins", "Watermelon"),
        SupplementProduct("Kaged", "In-Kaged", SupplementCategory.BCAA_EAA, "1 scoop (33 g)", 33f, 10, 0f, 2f, 0f, "Fermented BCAAs 2:1:1 (5g), L-citrulline 3g, CarnoSyn beta-alanine 1.6g, coconut water powder, taurine, L-tyrosine, PurCaf caffeine 124mg", "Fruit Punch"),
        SupplementProduct("MuscleBlaze", "BCAA Pro", SupplementCategory.BCAA_EAA, "1 scoop (15 g)", 15f, 7, 0f, 1f, 0f, "7 g vegan BCAAs (2:1:1 leucine/isoleucine/valine), 2.5 g L-glutamine, L-citrulline malate, electrolytes", "Watermelon"),
        SupplementProduct("MuscleBlaze", "EAA 8000", SupplementCategory.BCAA_EAA, "1 scoop (13.6 g)", 13.6f, 5, 0f, 0f, 0f, "8 g EAA blend (BCAA 2:1:1, lysine, threonine, phenylalanine, histidine, methionine, tryptophan), L-citrulline malate, coconut water powder, BioPerine", "Razz Lemonade"),
        SupplementProduct("MyProtein", "Essential BCAA 2:1:1", SupplementCategory.BCAA_EAA, "2 scoops (5 g)", 5f, 20, 0f, 0.5f, 0f, "BCAA 2:1:1 (L-leucine, L-isoleucine, L-valine), flavoring, sweetener (sucralose)", "Blue Raspberry"),
        SupplementProduct("Nutrabay", "Gold BCAA 4:1:1 with Electrolytes", SupplementCategory.BCAA_EAA, "1 scoop (9.5 g)", 9.5f, 28, 0f, 0.5f, 0f, "L-leucine, L-isoleucine, L-valine (4:1:1), electrolytes (sodium, potassium ~1000 mg), citric acid, sucralose", "Watermelon"),
        SupplementProduct("Optimum Nutrition", "Amino Energy", SupplementCategory.BCAA_EAA, "1 scoop (9 g)", 9f, 5, 0f, 1f, 0f, "Amino acid blend, green tea & green coffee caffeine, beta-alanine, citrulline", "Fruit Fusion"),
        SupplementProduct("Optimum Nutrition", "BCAA 5000 Powder", SupplementCategory.BCAA_EAA, "1 scoop (11.4 g)", 11.4f, 25, 0f, 2f, 0f, "5g branched-chain amino acids (leucine, isoleucine, valine 2:1:1), citric acid, natural & artificial flavor, sucralose", "Fruit Punch"),
        SupplementProduct("Optimum Nutrition", "Essential Amino Energy", SupplementCategory.BCAA_EAA, "1 scoop (9 g)", 9f, 10, 0f, 2f, 0f, "5g micronized amino blend (BCAAs, taurine, arginine, citrulline, beta-alanine, glutamine), 100mg caffeine from green tea & green coffee, natural flavor, sucralose", "Watermelon"),
        SupplementProduct("Scitron", "Advanced Vegan BCAA", SupplementCategory.BCAA_EAA, "1 scoop (9 g)", 9f, 5, 0f, 1f, 0f, "Vegan BCAA 2:1:1 (~6 g), electrolytes, natural flavour, sucralose; 0 g sugar", "Cola"),
        SupplementProduct("Scivation", "Xtend BCAA", SupplementCategory.BCAA_EAA, "1 scoop (13.5 g)", 13.5f, 0, 0f, 1f, 0f, "L-leucine, L-isoleucine, L-valine (2:1:1), L-glutamine, citrulline malate, electrolytes", "Blue Raspberry"),
        SupplementProduct("Scivation", "Xtend Elite BCAA", SupplementCategory.BCAA_EAA, "1 scoop (19.7 g)", 19.7f, 5, 0f, 0f, 0f, "7g BCAA (2:1:1), 3g citrulline nitrate (NO3-T), 2g PeakO2, 1.6g beta-alanine (CarnoSyn), glutamine, Sensoril ashwagandha, electrolytes", "Blue Raspberry"),
        SupplementProduct("Scivation", "Xtend Original BCAA", SupplementCategory.BCAA_EAA, "1 scoop (13.3 g)", 13.3f, 0, 0f, 0f, 0f, "7g BCAA (2:1:1 leucine/isoleucine/valine), 2.5g L-glutamine, 1g citrulline malate, electrolyte blend, vitamin B6", "Watermelon Explosion"),
        SupplementProduct("Scivation", "Xtend Ripped BCAA", SupplementCategory.BCAA_EAA, "1 scoop (16.5 g)", 16.5f, 15, 0f, 1f, 1f, "7g BCAA (2:1:1), 2.5g L-glutamine, 1g citrulline malate, CLA, L-carnitine, Capsimax, electrolyte blend", "Watermelon"),
        // ── PREWORKOUT ──
        SupplementProduct("Beast Life", "Beast Mode Pre-Workout", SupplementCategory.PREWORKOUT, "1 scoop (10.5 g)", 10.5f, 20, 0f, 2.5f, 0f, "L-Citrulline DL-Malate 2500 mg, beta-alanine 1500 mg, caffeine 200 mg, betaine 500 mg, L-theanine, L-tyrosine, black pepper extract", "Fruit Punch"),
        SupplementProduct("Beast Life", "Pre-Workout", SupplementCategory.PREWORKOUT, "1 scoop (10 g)", 10f, 10, 0f, 2f, 0f, "L-citrulline, beta-alanine, caffeine (200 mg), L-tyrosine, taurine", "Fruit Blast"),
        SupplementProduct("BigMuscles", "Freak Pre-Workout", SupplementCategory.PREWORKOUT, "1 scoop (13 g)", 13f, 15, 0f, 2f, 0f, "L-Citrulline DL-Malate 5 g, caffeine 300 mg, creatine 1.8 g, beta-alanine 1.6 g, L-arginine, L-tyrosine, taurine, betaine, green tea extract", "Sex on the Beach"),
        SupplementProduct("Cellucor", "C4 Original", SupplementCategory.PREWORKOUT, "1 scoop (6 g)", 6f, 5, 0f, 1f, 0f, "Beta-alanine, creatine nitrate, caffeine (150 mg), arginine AKG, N-acetyl tyrosine", "Fruit Punch"),
        SupplementProduct("Cellucor", "C4 Original Pre-Workout", SupplementCategory.PREWORKOUT, "1 scoop (6.5 g)", 6.5f, 5, 0f, 1f, 0f, "1.6g CarnoSyn beta-alanine, 1g creatine monohydrate, arginine AKG, N-acetyl-L-tyrosine, 150mg caffeine, vitamins C/B6/B12", "Fruit Punch"),
        SupplementProduct("Cellucor", "C4 Ripped Pre-Workout", SupplementCategory.PREWORKOUT, "1 scoop (5.7 g)", 5.7f, 5, 0f, 1f, 0f, "CarnoSyn beta-alanine, L-carnitine tartrate, 150mg caffeine, green coffee bean extract, Capsimax cayenne, L-tyrosine", "Cherry Limeade"),
        SupplementProduct("Kaged", "Pre-Kaged", SupplementCategory.PREWORKOUT, "1 scoop (32 g)", 32f, 20, 0f, 5f, 0f, "L-citrulline 6.5g, beta-alanine 1.6g, BCAAs, betaine, taurine, L-tyrosine, PurCaf organic caffeine 274mg, L-theanine, creatine HCl, Spectra", "Fruit Punch"),
        SupplementProduct("MuscleBlaze", "Pre Workout WrathX", SupplementCategory.PREWORKOUT, "1 scoop (17 g)", 17f, 8, 0f, 2f, 0f, "L-citrulline malate 3 g, beta-alanine, L-arginine, Creapure creatine, caffeine, L-theanine, taurine, EnXtra, BioPerine", "Fruit Fury"),
        SupplementProduct("Optimum Nutrition", "Gold Standard Pre-Workout", SupplementCategory.PREWORKOUT, "1 scoop (13.4 g)", 13.4f, 5, 0f, 3f, 0f, "3g creatine monohydrate, 1.6g CarnoSyn beta-alanine, 175mg caffeine, 375mg N-acetyl-L-tyrosine, AstraGin, natural flavor, sucralose", "Fruit Punch"),
        SupplementProduct("Scitron", "Volcano Pre-Workout", SupplementCategory.PREWORKOUT, "1 scoop (5 g)", 5f, 5, 0f, 1f, 0f, "Beta-alanine 4 g, L-arginine 1.5 g, caffeine 300 mg, ashwagandha, natural flavour; 0 g sugar", "Cherry Lemonade"),
        // ── BAR ──
        SupplementProduct("Avvatar", "Protein Wafer Bar", SupplementCategory.BAR, "1 bar (40 g)", 40f, 160, 10f, 16f, 6f, "Wafer (wheat flour), milk & whey protein, cocoa, maltitol, vegetable fat, 2 g fiber; no added sugar", "Chocolate"),
        SupplementProduct("Barebells", "Protein Bar", SupplementCategory.BAR, "1 bar (55 g)", 55f, 200, 20f, 18f, 8f, "Milk protein blend (caseinate, WPC, WPI), collagen, roasted peanuts, maltitol, cocoa butter", "Salty Peanut"),
        SupplementProduct("Grenade", "Carb Killa", SupplementCategory.BAR, "1 bar (60 g)", 60f, 214, 20f, 17f, 8f, "Milk protein, whey protein, collagen, cocoa, humectants, sucralose", "Chocolate Chip Salted Caramel"),
        SupplementProduct("Grenade", "Carb Killa Protein Bar", SupplementCategory.BAR, "1 bar (60 g)", 60f, 225, 20f, 22.5f, 9.5f, "Milk & whey protein blend, bovine collagen, milk chocolate, caramel, humectants", "Salted Caramel"),
        SupplementProduct("MuscleBlaze", "Hi-Protein Bar (30g Protein)", SupplementCategory.BAR, "1 bar (70 g)", 70f, 350, 30f, 46f, 6.5f, "Milk protein blend, whey protein, soluble fibre, cocoa, almonds, sweeteners", "Choco Almond"),
        SupplementProduct("MuscleBlaze", "Protein Bar", SupplementCategory.BAR, "1 bar (60 g)", 60f, 213, 20f, 21f, 6f, "Whey protein, milk protein, soy protein, dates, nuts, cocoa", "Chocolate"),
        SupplementProduct("Nourish Vitals", "Breakfast Protein Bars", SupplementCategory.BAR, "1 bar (30 g)", 30f, 130, 5f, 18f, 5f, "Rolled oats, brown rice flakes, foxtail millet, quinoa flakes, roasted seeds, dried fruits, dates; no added sugar or artificial flavours", "Assorted (nuts & seeds)"),
        SupplementProduct("Quest", "Protein Bar", SupplementCategory.BAR, "1 bar (60 g)", 60f, 200, 20f, 21f, 8f, "Protein blend (whey & milk isolate), soluble corn fibre, almonds, erythritol, cocoa", "Cookies & Cream"),
        SupplementProduct("RiteBite", "Choco Delite Energy Bar", SupplementCategory.BAR, "1 bar (40 g)", 40f, 175, 6f, 25f, 6f, "Chocolate coating, almonds, cashews, golden raisins, oats, protein blend", "Choco Delite"),
        SupplementProduct("RiteBite", "Daily 10g Protein Bar", SupplementCategory.BAR, "1 bar (38 g)", 38f, 165, 10f, 19f, 6f, "Soy & whey protein blend, oats, almonds, cocoa, fibre, vitamins & minerals", "Choco Almond"),
        SupplementProduct("RiteBite", "Green Coffee Beans Protein Bar", SupplementCategory.BAR, "1 bar (67 g)", 67f, 285, 20f, 30f, 9f, "Green coffee bean extract, whey/soy protein blend, oats, almonds, cocoa", "Green Coffee Beans"),
        SupplementProduct("RiteBite", "Max Protein Daily", SupplementCategory.BAR, "1 bar (50 g)", 50f, 180, 10f, 22f, 6f, "Soy protein, milk protein, dates, nuts, cocoa, oats", "Choco Slim"),
        SupplementProduct("RiteBite", "Max Protein Professional", SupplementCategory.BAR, "1 bar (60 g)", 60f, 232, 20f, 22f, 7f, "Whey protein, milk protein, soy protein, nuts, cocoa, dates", "Choco Whey"),
        SupplementProduct("RiteBite", "Professional 30g Protein Bar", SupplementCategory.BAR, "1 bar (100 g)", 100f, 361, 30f, 45f, 14.7f, "Whey & soy protein blend, almonds, cocoa, fibre, vitamins & minerals, no added sugar", "Choco Almond (Ultimate)"),
        SupplementProduct("The Whole Truth", "20g Protein Bar", SupplementCategory.BAR, "1 bar (67 g)", 67f, 347, 20f, 30f, 16f, "Dates, peanuts, whey protein, cocoa, almonds; 5-7 clean ingredients, no added sugar", "Peanut Cocoa"),
        SupplementProduct("The Whole Truth", "Protein Bar", SupplementCategory.BAR, "1 bar (52 g)", 52f, 200, 10f, 20f, 9f, "Dates, whey protein, peanuts, almonds, cocoa, no added sugar", "Chocolate Brownie"),
        SupplementProduct("Yoga Bar", "20g Protein Bar", SupplementCategory.BAR, "1 bar (70 g)", 70f, 320, 21f, 27f, 14.4f, "Whey & lentil protein blend, almonds, dates, prebiotic fibre, cocoa, ricebran oil", "Chocolate Brownie"),
        SupplementProduct("Yoga Bar", "Daily 10g Protein Bar", SupplementCategory.BAR, "1 bar (50 g)", 50f, 194, 10f, 27f, 4.9f, "Dates, whey protein, oats, cranberry, almonds, prebiotic fibre, cocoa", "Chocolate Cranberry"),
        // ── PEANUT_BUTTER ──
        SupplementProduct("Alpino", "High Protein Dark Chocolate Peanut Butter", SupplementCategory.PEANUT_BUTTER, "1 tbsp (15 g)", 15f, 86, 4.5f, 4.8f, 5.4f, "Peanuts, dark chocolate, whey/soy protein, cocoa", "Dark chocolate crisp, 30% protein"),
        SupplementProduct("Alpino", "High Protein Peanut Butter", SupplementCategory.PEANUT_BUTTER, "1 tbsp (15 g)", 15f, 84, 5.2f, 3f, 5.7f, "Roasted peanuts, soy protein isolate, sugar", "Crunch, 35% vegan protein"),
        SupplementProduct("Alpino", "Natural Peanut Butter", SupplementCategory.PEANUT_BUTTER, "1 tbsp (15 g)", 15f, 90, 4.5f, 2.5f, 7.5f, "100% roasted peanuts (no added sugar or salt)", "Smooth, unsweetened"),
        SupplementProduct("Beast Life", "Natural Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (30 g)", 30f, 183, 8f, 6f, 15f, "100% roasted peanuts (Junagadh) with real cocoa; no palm oil, no added sugar, no artificial ingredients", "Chocolate Crunchy"),
        SupplementProduct("DiSano", "All Natural Peanut Butter", SupplementCategory.PEANUT_BUTTER, "1 tbsp (15 g)", 15f, 88, 4.5f, 2.7f, 7.4f, "100% roasted peanuts (no salt, sugar or hydrogenated fat)", "Creamy, unsweetened, 30% protein"),
        SupplementProduct("MuscleBlaze", "High Protein Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (34 g)", 34f, 193, 9f, 13f, 13.6f, "Roasted peanuts, whey protein concentrate, cocoa, sweetener", "Dark Chocolate"),
        SupplementProduct("MyFitness", "Chocolate Peanut Butter", SupplementCategory.PEANUT_BUTTER, "1 tbsp (15 g)", 15f, 90, 3.9f, 4.2f, 6.6f, "Roasted peanuts, sugar, cocoa solids, cocoa butter", "Chocolate, smooth (28% protein)"),
        SupplementProduct("MyFitness", "High Protein Peanut Butter", SupplementCategory.PEANUT_BUTTER, "1 tbsp (15 g)", 15f, 89, 4.5f, 3f, 6.5f, "Roasted peanuts, whey protein concentrate, sugar", "Creamy, ~30% protein with added whey"),
        SupplementProduct("MyFitness", "Original Peanut Butter", SupplementCategory.PEANUT_BUTTER, "1 tbsp (15 g)", 15f, 95, 3.8f, 3.5f, 7.3f, "Roasted peanuts, sugar, salt", "Smooth, 25% protein"),
        SupplementProduct("MyFitness", "Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (33 g)", 33f, 190, 8f, 9f, 14f, "Roasted peanuts, cocoa, cane sugar, sea salt", "Chocolate"),
        SupplementProduct("Nourish Vitals", "High Protein Peanut Butter (Extra Crunchy)", SupplementCategory.PEANUT_BUTTER, "2 tbsp (32 g)", 32f, 185, 10f, 5f, 14f, "Roasted peanuts, whey protein concentrate; no palm oil, no added sugar or salt", "Classic Roast Extra Crunchy"),
        SupplementProduct("Pintola", "All Natural Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (32 g)", 32f, 190, 8f, 6f, 15f, "100% roasted peanuts", "Creamy"),
        SupplementProduct("Pintola", "All Natural Peanut Butter (Unsweetened)", SupplementCategory.PEANUT_BUTTER, "1 tbsp (15 g)", 15f, 90, 4.5f, 2.5f, 7.5f, "100% roasted peanuts (no added sugar, salt or hydrogenated oil)", "Creamy, unsweetened"),
        SupplementProduct("Pintola", "Classic Peanut Butter (Crunchy)", SupplementCategory.PEANUT_BUTTER, "1 tbsp (15 g)", 15f, 92, 3.9f, 3.3f, 6.9f, "Roasted peanuts, sugar, salt", "Crunchy, American-style"),
        SupplementProduct("Pintola", "High Protein Dark Chocolate Peanut Butter", SupplementCategory.PEANUT_BUTTER, "1 tbsp (15 g)", 15f, 88, 4.5f, 4.8f, 5.4f, "Peanuts (60%), dark chocolate (30%), whey protein concentrate, cocoa, emulsifier (INS 322)", "Dark chocolate, crunchy"),
        SupplementProduct("Pintola", "High Protein Peanut Butter", SupplementCategory.PEANUT_BUTTER, "1 tbsp (15 g)", 15f, 91, 5f, 4f, 6f, "Roasted peanuts, whey protein concentrate, sugar", "Creamy, ~30% protein"),
        SupplementProduct("The Whole Truth", "Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (32 g)", 32f, 190, 9f, 6f, 15f, "Roasted peanuts, no added sugar or palm oil", "Classic"),
        SupplementProduct("The Whole Truth", "Peanut Butter (Classic)", SupplementCategory.PEANUT_BUTTER, "1 tbsp (15 g)", 15f, 90, 4.2f, 2.1f, 7.5f, "Roasted peanuts (no added sugar, palm oil or preservatives)", "Classic crunchy, clean-label"),
        SupplementProduct("The Whole Truth", "Unsweetened Peanut Butter", SupplementCategory.PEANUT_BUTTER, "2 tbsp (32 g)", 32f, 190, 8f, 6f, 16f, "100% roasted peanuts; no added sugar, no palm oil, crunchy", "Unsweetened Crunchy"),
        // ── PROTEIN_FOOD ──
        SupplementProduct("HK Vitals", "ProteinUp Women", SupplementCategory.PROTEIN_FOOD, "1 scoop (25 g)", 25f, 90, 12f, 8f, 1f, "Whey + soy protein, marine collagen builder blend, biotin, vitamin C & E, garcinia & green tea extract; no added sugar", "Chocolate"),
        SupplementProduct("Labrada", "Lean Body Hi-Protein Meal Replacement", SupplementCategory.PROTEIN_FOOD, "2 scoops (70 g)", 70f, 285, 35f, 20f, 7f, "Protein blend (whey concentrate, calcium caseinate, egg albumin), sunflower oil, dietary fiber, vitamin & mineral blend, cocoa", "Chocolate"),
        SupplementProduct("MuscleBlaze", "High Protein Oats", SupplementCategory.PROTEIN_FOOD, "100 g", 100f, 380, 22f, 55f, 6f, "Rolled oats, whey protein, fruits & seeds, cocoa, probiotics", "Dark Chocolate"),
        SupplementProduct("True Elements", "Protein Muesli", SupplementCategory.PROTEIN_FOOD, "1 serving (40 g)", 40f, 160, 9f, 24f, 3f, "Rolled oats, soy protein, nuts, seeds, raisins", "Original"),
        SupplementProduct("True Elements", "Protein Muesli (Super Muesli)", SupplementCategory.PROTEIN_FOOD, "100 g", 100f, 385, 24f, 52f, 8f, "Wholegrain oats, soy crispies, almonds, seeds, black currant, 15% millets", "Almonds, Seeds & Black Currant"),
        SupplementProduct("Yoga Bar", "High Protein Muesli", SupplementCategory.PROTEIN_FOOD, "1 serving (50 g)", 50f, 193, 12f, 27f, 5.4f, "Wholegrains, whey protein, almonds, seeds, cranberry, probiotics, no refined sugar", "Choco Almond & Cranberry"),
        SupplementProduct("Yoga Bar", "High Protein Oats", SupplementCategory.PROTEIN_FOOD, "1 serving (50 g)", 50f, 180, 12f, 28f, 3.8f, "Rolled oats, whey protein, chia/sunflower/pumpkin seeds, cocoa, probiotics, no added sugar", "Dark Chocolate"),
        SupplementProduct("Yoga Bar", "Protein Oats", SupplementCategory.PROTEIN_FOOD, "1 serving (50 g)", 50f, 190, 10f, 32f, 3f, "Rolled oats, soy protein, almonds, cocoa", "Chocolate"),
        // ── RTD ──
        SupplementProduct("Amul", "High Protein Buttermilk", SupplementCategory.RTD, "1 pack (200 ml)", 200f, 108, 15f, 8f, 1f, "Toned milk, whey protein, milk solids, spices, salt", "Spiced/masala, 15 g protein"),
        SupplementProduct("Amul", "High Protein Lassi", SupplementCategory.RTD, "1 bottle (200 ml)", 200f, 140, 15f, 18f, 0.5f, "Toned milk, milk solids, sugar, active cultures", "Lassi"),
        SupplementProduct("Amul", "High Protein Milk", SupplementCategory.RTD, "1 pack (250 ml)", 250f, 225, 35f, 20f, 0.5f, "Toned milk, milk protein concentrate", "Plain, 35 g protein per pack"),
        SupplementProduct("Amul", "High Protein Plain Lassi", SupplementCategory.RTD, "1 pack (200 ml)", 200f, 107, 15f, 10f, 0.5f, "Toned milk, whey protein, no added sugar", "Plain, low fat, 15 g protein"),
        SupplementProduct("Avvatar", "Protein Cold Coffee (RTD)", SupplementCategory.RTD, "1 pack (250 ml)", 250f, 120, 15f, 10f, 1.5f, "Toned milk, milk & whey protein, coffee solids, cocoa, sweetener (sucralose); no added sugar", "Classic Cold Coffee"),
        SupplementProduct("Epigamia", "Turbo 25g Protein Milkshake", SupplementCategory.RTD, "1 bottle (250 ml)", 250f, 136, 25f, 9f, 0f, "Milk, milk protein concentrate, cocoa, sucralose", "Chocolate, zero fat, 25 g protein"),
    )
}
