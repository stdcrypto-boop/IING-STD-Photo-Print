package com.iingstd.photoprint;
import android.app.*;import android.os.*;import android.widget.*;import android.graphics.Color;import java.net.*;import java.io.*;
public class MainActivity extends Activity {
 android.net.nsd.NsdManager nsd; android.net.nsd.NsdManager.DiscoveryListener listener;
 boolean resolving;
 void discover(EditText uri, TextView status){
 if(listener!=null)return;
 nsd=(android.net.nsd.NsdManager)getSystemService(NSD_SERVICE);
 listener=new android.net.nsd.NsdManager.DiscoveryListener(){
 public void onDiscoveryStarted(String t){runOnUiThread(()->status.setText("Mencari printer IPP..."));}
 public void onDiscoveryStopped(String t){}
 public void onStartDiscoveryFailed(String t,int e){runOnUiThread(()->{listener=null;status.setText("Pencarian gagal: "+e);});}
 public void onStopDiscoveryFailed(String t,int e){}
 public void onServiceLost(android.net.nsd.NsdServiceInfo s){}
 public void onServiceFound(android.net.nsd.NsdServiceInfo s){runOnUiThread(()->{if(resolving||listener==null)return;resolving=true;nsd.resolveService(s,new android.net.nsd.NsdManager.ResolveListener(){
 public void onResolveFailed(android.net.nsd.NsdServiceInfo i,int e){runOnUiThread(()->{resolving=false;status.setText("Resolusi printer gagal: "+e);});}
 public void onServiceResolved(android.net.nsd.NsdServiceInfo i){runOnUiThread(()->{resolving=false;if(listener==null||i.getHost()==null)return;try{byte[] rp=i.getAttributes().get("rp");String path=rp==null?"ipp/print":new String(rp,java.nio.charset.StandardCharsets.UTF_8);if(!path.startsWith("/"))path="/"+path;String address=new URI("ipp",null,i.getHost().getHostAddress(),i.getPort(),path,null,null).toString();uri.setText(address);status.setText("Ditemukan: "+i.getServiceName()+" — tekan Uji koneksi");stopDiscovery();}catch(Exception e){status.setText(e.toString());}});}
 });});}
 };
 try{nsd.discoverServices("_ipp._tcp.",android.net.nsd.NsdManager.PROTOCOL_DNS_SD,listener);new Handler(Looper.getMainLooper()).postDelayed(()->{if(listener!=null){stopDiscovery();status.setText("Pencarian selesai. Masukkan IP manual jika belum ditemukan.");}},15000);}catch(Exception e){listener=null;status.setText(e.toString());}
 }
 void stopDiscovery(){if(listener!=null){try{nsd.stopServiceDiscovery(listener);}catch(Exception ignored){}listener=null;}}
 @Override protected void onDestroy(){stopDiscovery();super.onDestroy();}

 public void onCreate(Bundle b){super.onCreate(b); LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(32,64,32,32);setContentView(l); TextView title=new TextView(this);title.setText("IING STD Photo Print");title.setTextSize(26);title.setTextColor(0xff007f80);l.addView(title); EditText uri=new EditText(this);uri.setHint("ipp://192.168.1.100:631/ipp/print");uri.setText(getPreferences(0).getString("uri",""));l.addView(uri);Button probe=new Button(this);probe.setText("Uji koneksi printer IPP");l.addView(probe);TextView status=new TextView(this);l.addView(status);Button scan=new Button(this);scan.setText("Cari printer Wi-Fi");l.addView(scan);scan.setOnClickListener(v->discover(uri,status));probe.setOnClickListener(v->{String s=uri.getText().toString().trim();getPreferences(0).edit().putString("uri",s).apply();status.setText("Memeriksa printer...");probe.setEnabled(false);new Thread(()->{String result;try{result=Ipp.probe(s);}catch(Exception e){result="Gagal: "+e.getMessage();}String r=result;runOnUiThread(()->{status.setText(r);probe.setEnabled(true);});}).start();}); }
}
