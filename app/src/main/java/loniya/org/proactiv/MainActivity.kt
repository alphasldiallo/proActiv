package loniya.org.proactiv

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import io.matchmore.sdk.Matchmore
import io.matchmore.sdk.api.models.Publication
import io.matchmore.sdk.api.models.Subscription

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //Matchmore.com  - Test
        val API_KEY =
            "YOUR API_KEY"
        if (!Matchmore.isConfigured()) {
            Matchmore.config(this, API_KEY, false)

        }

        //FCM to trigger notifications
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("testble", "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token

                Log.d("testble", token.toString())
            })

        checkLocationPermission()
        createSub()
        getMatches()

        val btn = findViewById<Button>(R.id.publish)
        btn.setOnClickListener()
        {
            createPub()
        }
    }

    private fun createSub() {
        Matchmore.instance.apply {
            startUsingMainDevice({ device ->
                val subscription = Subscription("beacon_test", 0.2, 3000.0)
                //Matches are being refreshed every n seconds
                subscription.matchTTL = 2.0

                //Matches are being refreshed every time the Subcriber moves
                subscription.matchDTL = 2.0

                createSubscriptionForMainDevice(subscription, { result ->
                    Log.d("testble", "Subscription created successfully on topic: " + result.topic.toString())
                    val status = findViewById<TextView>(R.id.status)
                    status.text = Matchmore.instance.locationManager.lastLocation.toString()

                }, Throwable::printStackTrace)
            }, Throwable::printStackTrace)
        }
    }

    private fun createPub() {
        Matchmore.instance.apply {
            startUsingMainDevice({ device ->

                val pub = Publication("beacon_test", 1.0, 30.0)

                createPublicationForMainDevice(pub,
                    { result ->
                        Log.d("testb", "Publication made successfully with properties " + pub.properties.toString())
                    }, Throwable::printStackTrace
                )
            }, Throwable::printStackTrace)
        }
    }


    private fun getMatches() {

        Matchmore.instance.apply {

            //Start fetching matches
            matchMonitor.addOnMatchListener { matches, _ ->
                //We should get there every time a match occur

                Log.d("testb", "${matches}")

                val status = findViewById<TextView>(R.id.status)
                status.text = "Shop name = ${
                matches.first().publication!!.properties["shop_name"].toString()
                }"
            }
            matchMonitor.startPollingMatches(100)
        }

    }

    private fun checkLocationPermission() {
        val permissionListener = object : PermissionListener {
            @SuppressLint("MissingPermission")
            override fun onPermissionGranted() {
                Matchmore.instance.apply {
                    startUpdatingLocation()
                    startRanging()

                    Log.d("debug", "start ranging")
                }

            }

            override fun onPermissionDenied(deniedPermissions: ArrayList<String>) {
                Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
        TedPermission.with(this)
            .setPermissionListener(permissionListener)
            .setDeniedMessage("Permission Denied")
            .setPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
            .check()
    }
}
