package org.who.ddccverifier

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import kotlinx.coroutines.*
import org.who.ddccverifier.databinding.ActivityMainBinding
import org.who.ddccverifier.services.trust.TrustRegistry

/**
 * Screen / Class flow:
 *
 * ┌──────────────────────────────────────────────────┐      ┌────────────────┐ ┌──────────┐
 * │                  MainActivity                    │      │ TrustRegistry  ├↔┤ KeyUtils │
 * └──────────────────────────────────────────────────┘      └─────────────╥──┘ └──────────┘
 * ┌──────────────┐ ┌──────────────┐ ┌────────────────┐                    ║
 * │ HomeFragment ├→┤ ScanFragment ├→┤ ResultFragment │←─DDCC UI Card──────╫─────────┐
 * └──────────────┘ └─────┬──▲─────┘ └────────┬───────┘                    ║         │
 *                   Image│  │QRContent       │QRContent                   ║         │
 *                  ┌─────▼──┴─────┐     ┌────▼───────┐                    ║         │
 *                  │   QRFinder   │     │ QRDecoder  │                    ║         │
 *                  └──────────────┘     └────┬───────┘                    ║         │
 *                                            │QRContent                   ║         │
 *             ┌─────────────────┬────────────┴─────┬───────────────────┐  ║         │
 *  ╔══════════╪═════════════════╪══════════════════╪═══════════════════╪══╩══════╗  │
 *  ║ ┌────────▼───────┐  ┌──────▼──────┐   ┌───────▼───────┐   ┌───────▼───────┐ ║  │
 *  ║ │  HCertVerifier │  │ SHCVerifier │   │ DivocVerifier │   │ ICAOVerifier  │ ║  │
 *  ║ └────┬───────────┘  └──────┬──────┘   └───────┬───────┘   └───────┬───────┘ ║  │
 *  ╚══════╪═════════════════════╪══════════════════╪═══════════════════╪═════════╝  │
 *         │HCERT CBOR           │JWT               │JSONLD             │iJSON       │
 *    ┌────▼───────────┐ ┌───────▼───────┐ ┌────────▼─────────┐ ┌───────▼─────────┐  │
 *    │ CBORTranslator │ │ JWTTranslator │ │ JSONLDTranslator │ │ IJsonTranslator │  │
 *    └──┬──────────┬──┘ └───────┬───────┘ └────────┬─────────┘ └───────┬─────────┘  │
 *   FHIR│Struct    │DCC CWT     │FHIR DDCC         │FHIR DDCC          │FHIR DDCC   │
 * ┌─────▼────┐┌────▼─────┐      │                  │                   │            │
 * │ WHO2FHIR ││ DCC2FHIR │      │                  │                   │            │
 * └─────┬────┘└────┬─────┘      │                  │                   │            │
 *   FHIR│DDCC  FHIR│DDCC        │                  │                   │            │
 *       └──────────┴────────────┴────────────┬─────┴───────────────────┘            │
 *                                            │                                      │
 *  ┌──────────────┐                          │DDCC Composite                        │
 *  │ Assets       │ ┌────────────────┐  ┌────▼───────────┐                          │
 *  │ - ModelInfo  ├↔┤CQLLibraryLoader├←→┤ CQLEvaluator   │                          │
 *  │ - FHIRHelper │ └────────────────┘  └────┬───────────┘                          │
 *  │ - DDCCPass   │                          │DDCC Composite                        │
 *  └──────────────┘                     ┌────▼───────────┐                          │
 *                                       │ DDCCFormatter  ├→─DDCC UI Card────────────┘
 *                                       └────────────────┘
 */
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    fun init() = runBlocking {
        var viewModelJob = Job()
        val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

        uiScope.launch {
            withContext(Dispatchers.IO) {
                backgroundInit()
            }
        }
    }

    suspend fun backgroundInit() {
        // Triggers Networking
        TrustRegistry.init()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}