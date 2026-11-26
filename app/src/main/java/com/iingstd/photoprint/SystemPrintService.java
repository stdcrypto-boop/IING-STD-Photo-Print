package com.iingstd.photoprint;
import android.print.*;
import android.printservice.*;
import android.os.*;
import java.io.*;
import java.util.*;
public class SystemPrintService extends PrintService {
 final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
 String address(){return getSharedPreferences("MainActivity",0).getString("uri","").trim();}
 @Override protected void onConnected(){Ipp.cacheDirectory=getCacheDir();Ipp.connectivity=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);}
 @Override protected PrinterDiscoverySession onCreatePrinterDiscoverySession(){return new PrinterDiscoverySession(){
 boolean active; int generation;
 @Override public void onStartPrinterDiscovery(List<PrinterId> priority){active=true;refresh();}
 void refresh(){final int token=++generation;final String uri=address();worker.execute(()->{String report;try{report=Ipp.probe(uri);}catch(Exception e){report=null;}final String caps=report;new Handler(Looper.getMainLooper()).post(()->{if(!active||token!=generation)return;PrinterId id=generatePrinterId("configured-ipp");if(caps==null){removePrinters(Collections.singletonList(id));return;}
 PrinterCapabilitiesInfo.Builder b=new PrinterCapabilitiesInfo.Builder(id);
 String media=Ipp.capability(caps,"media-supported");boolean any=false;
 if(media.contains("iso_a4_210x297mm")){b.addMediaSize(PrintAttributes.MediaSize.ISO_A4,true);any=true;}
 if(media.contains("custom_f4_210x330mm")||media.contains("210x330")){b.addMediaSize(new PrintAttributes.MediaSize("F4","F4",8268,12992),!any);any=true;}
 if(media.contains("na_legal_8.5x14in")){b.addMediaSize(PrintAttributes.MediaSize.NA_LEGAL,!any);any=true;}
 if(!any){removePrinters(Collections.singletonList(id));return;}
 b.addResolution(new PrintAttributes.Resolution("default","Default",300,300),true);
 b.setMinMargins(new PrintAttributes.Margins(197,197,197,197));
 String colors=Ipp.capability(caps,"print-color-mode-supported");int modes=colors.contains("color")?PrintAttributes.COLOR_MODE_COLOR|PrintAttributes.COLOR_MODE_MONOCHROME:PrintAttributes.COLOR_MODE_MONOCHROME;
 b.setColorModes(modes,PrintAttributes.COLOR_MODE_MONOCHROME);
 addPrinters(Collections.singletonList(new PrinterInfo.Builder(id,"IING STD IPP",PrinterInfo.STATUS_IDLE).setCapabilities(b.build()).build()));});});}
 @Override public void onStopPrinterDiscovery(){active=false;generation++;}
 @Override public void onValidatePrinters(List<PrinterId> ids){if(active)refresh();}
 @Override public void onStartPrinterStateTracking(PrinterId id){if(active)refresh();}
 @Override public void onStopPrinterStateTracking(PrinterId id){}
 @Override public void onDestroy(){active=false;generation++;}
 };}
 @Override protected void onPrintJobQueued(PrintJob job){
 if(!job.start())return;
 PrintAttributes a=job.getInfo().getAttributes();PrintAttributes.MediaSize size=a.getMediaSize();String id=size.getId();String media=id.equals("F4")?"custom_f4_210x330mm":id.equals(PrintAttributes.MediaSize.NA_LEGAL.getId())?"na_legal_8.5x14in":"iso_a4_210x297mm";
 final boolean landscape=!size.isPortrait();final String color=a.getColorMode()==PrintAttributes.COLOR_MODE_COLOR?"color":"monochrome";final int copies=job.getInfo().getCopies();final String uri=address();final ParcelFileDescriptor fd=job.getDocument().getData();
 if(fd==null){job.fail("Dokumen tidak tersedia");return;}
 worker.execute(()->{String error=null;try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){Ipp.printOriented(uri,"application/pdf",in,media,0,color,"",copies,0,0,landscape);}catch(Exception e){error=e.getMessage()==null?"Pengiriman gagal":e.getMessage();}final String result=error;new Handler(Looper.getMainLooper()).post(()->{if(job.isCancelled())return;if(result==null)job.complete();else job.fail(result);});});
 }
 @Override protected void onRequestCancelPrintJob(PrintJob job){job.cancel();}
}
