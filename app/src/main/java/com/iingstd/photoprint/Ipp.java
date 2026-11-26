package com.iingstd.photoprint;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;
final class Ipp {
 static java.io.File cacheDirectory;
 static android.net.ConnectivityManager connectivity;
 static HttpURLConnection printerConnection(URL url)throws IOException{
 if(connectivity==null)throw new IOException("Jaringan belum siap");
 android.net.Network fallback=null;
 for(android.net.Network network:connectivity.getAllNetworks()){
 android.net.NetworkCapabilities caps=connectivity.getNetworkCapabilities(network);
 if(caps==null||!caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI))continue;
 if(fallback==null)fallback=network;
 android.net.LinkProperties links=connectivity.getLinkProperties(network);
 if(links==null)continue;
 try{java.net.InetAddress target=network.getByName(url.getHost());for(android.net.RouteInfo route:links.getRoutes()){if(!route.isDefaultRoute()&&route.matches(target))return (HttpURLConnection)network.openConnection(url,Proxy.NO_PROXY);}}catch(java.net.UnknownHostException ignored){}
 }
 if(fallback!=null)return (HttpURLConnection)fallback.openConnection(url,Proxy.NO_PROXY);
 throw new IOException("Sambungkan HP ke Wi-Fi printer terlebih dahulu");
 }

 static URI printerUri(String address)throws IOException{
 try{URI u=new URI(address);if((!"ipp".equals(u.getScheme())&&!"ipps".equals(u.getScheme()))||u.getHost()==null||u.getUserInfo()!=null||u.getFragment()!=null||u.getQuery()!=null||u.getPort()==0||u.getPort()>65535)throw new IOException("Alamat IPP tidak valid");return u;}catch(URISyntaxException e){throw new IOException("Alamat IPP tidak valid",e);}
 }
 static void checkResponse(HttpURLConnection c)throws IOException{
 if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode()+"; Location: "+c.getHeaderField("Location"));
 String type=c.getContentType();if(type!=null&&!type.split(";",2)[0].trim().equalsIgnoreCase("application/ipp")&&!type.split(";",2)[0].trim().equalsIgnoreCase("application/octet-stream"))throw new IOException("Respons bukan IPP: "+type+" dari "+c.getURL());
 }
 static String capability(String report,String key){for(String line:report.split("\\n"))if(line.startsWith(key+": "))return line.substring(key.length()+2);return "";}
 static void requireSupported(String report,String key,String value,String label)throws IOException{
 if(value.isEmpty())return;
 String advertised=capability(report,key);
 if(!advertised.isEmpty()&&!java.util.Arrays.asList(advertised.split(", ")).contains(value))throw new IOException(label+" tidak didukung printer: "+value+". Pilihan tersedia: "+advertised);
 }
 static int rasterPixel(int pixel,boolean gray){if(!gray)return pixel&0xffffff;return (((pixel>>16)&255)*299+((pixel>>8)&255)*587+(pixel&255)*114+500)/1000;}
 static void writeRasterPixel(DataOutputStream out,int pixel,boolean gray)throws IOException{if(!gray){out.writeByte(pixel>>16);out.writeByte(pixel>>8);}out.writeByte(pixel);}
 static void writeRasterRow(DataOutputStream out,int[] row,boolean gray)throws IOException{
 out.writeByte(0);
 for(int x=0;x<row.length;){
 int pixel=rasterPixel(row[x],gray),run=1;
 while(run<128&&x+run<row.length&&rasterPixel(row[x+run],gray)==pixel)run++;
 if(run>1){out.writeByte(run-1);writeRasterPixel(out,pixel,gray);x+=run;continue;}
 int count=1;
 while(count<128&&x+count<row.length){int pos=x+count;if(pos+1<row.length&&rasterPixel(row[pos],gray)==rasterPixel(row[pos+1],gray))break;count++;}
 out.writeByte(count==1?0:257-count);for(int j=0;j<count;j++)writeRasterPixel(out,rasterPixel(row[x+j],gray),gray);x+=count;
 }
 }
 static byte[] rasterHeader(int w,int h,int dpi,int mmw,int mmh,boolean gray,int pages){
 int channels=gray?1:3;
 java.nio.ByteBuffer header=java.nio.ByteBuffer.allocate(1796);header.put("PwgRaster".getBytes(java.nio.charset.StandardCharsets.US_ASCII));header.putInt(276,dpi);header.putInt(280,dpi);header.putInt(340,1);header.putInt(352,(int)Math.round(mmw*72.0/2540));header.putInt(356,(int)Math.round(mmh*72.0/2540));header.putInt(372,w);header.putInt(376,h);header.putInt(384,8);header.putInt(388,8*channels);header.putInt(392,w*channels);header.putInt(400,gray?18:19);header.putInt(420,channels);header.putInt(452,pages);header.putInt(456,1);header.putInt(460,1);header.putInt(464,0);header.putInt(468,0);header.putInt(472,w);header.putInt(476,h);
 return header.array();
 }
 static void rasterPdf(InputStream input,File target,String report,String media,int first,int last,String color,boolean actualSize)throws Exception{rasterPdf(input,target,report,media,first,last,color,actualSize,false);}
 static void rasterPdf(InputStream input,File target,String report,String media,int first,int last,String color,boolean actualSize,boolean landscape)throws Exception{rasterPdf(input,target,report,media,first,last,color,actualSize,landscape,1);}
 static void rasterPdf(InputStream input,File target,String report,String media,int first,int last,String color,boolean actualSize,boolean landscape,int copies)throws Exception{
 String types=capability(report,"pwg-raster-document-type-supported");
 boolean gray="monochrome".equals(color);int channels=gray?1:3;
 if(!java.util.Arrays.asList(types.split(", ")).contains(gray?"sgray_8":"srgb_8"))throw new IOException("Tipe warna PWG Raster tidak didukung");
 String resolutions=capability(report,"pwg-raster-document-resolution-supported");
 int dpi=0;for(int candidate:new int[]{300,200,150})if(java.util.Arrays.asList(resolutions.split(", ")).contains(candidate+"x"+candidate+" dpi")){dpi=candidate;break;}if(dpi==0)throw new IOException("Tidak ada resolusi PWG Raster yang didukung aplikasi (150/200/300 dpi)");
 int mmw=21000,mmh=29700;if(media.equals("custom_10rmini_210x252mm"))mmh=25200;else if(media.equals("custom_f4_210x330mm"))mmh=33000;else if(media.equals("na_legal_8.5x14in")){mmw=21590;mmh=35560;}else if(media.equals("na_index-4x6_4x6in")){mmw=10160;mmh=15240;}
 int w=(int)Math.round(mmw*dpi/2540.0),h=(int)Math.round(mmh*dpi/2540.0);
 File source=File.createTempFile("raster-source-",".pdf",cacheDirectory);
 try{try(OutputStream out=new FileOutputStream(source)){byte[] b=new byte[65536];int n;while((n=input.read(b))!=-1)out.write(b,0,n);}
 try(android.graphics.pdf.PdfRenderer renderer=new android.graphics.pdf.PdfRenderer(android.os.ParcelFileDescriptor.open(source,android.os.ParcelFileDescriptor.MODE_READ_ONLY));DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(target)))){
 int begin=first>0?first-1:0,end=last>0?last:renderer.getPageCount();if(begin>=end||end>renderer.getPageCount())throw new IOException("Rentang PDF tidak valid");
 out.writeBytes("RaS2");
 for(int copy=0;copy<copies;copy++)for(int i=begin;i<end;i++){try(android.graphics.pdf.PdfRenderer.Page page=renderer.openPage(i)){
 out.write(rasterHeader(w,h,dpi,mmw,mmh,gray,Math.multiplyExact(end-begin,copies)));
 android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888);
 try{bitmap.eraseColor(android.graphics.Color.WHITE);float scale=actualSize?dpi/72f:Math.min((float)w/page.getWidth(),(float)h/page.getHeight());if(actualSize&&(Math.abs(page.getWidth()*scale-w)>dpi/72f+1||Math.abs(page.getHeight()*scale-h)>dpi/72f+1))throw new IOException("Ukuran PDF tidak sama dengan kertas; skala asli ditolak");android.graphics.Matrix matrix=new android.graphics.Matrix();matrix.setScale(scale,scale);if(!actualSize)matrix.postTranslate((w-page.getWidth()*scale)/2,(h-page.getHeight()*scale)/2);if(landscape){float s=Math.min((float)h/page.getWidth(),(float)w/page.getHeight());matrix.reset();matrix.setScale(s,s);matrix.postRotate(90);matrix.postTranslate((w+page.getHeight()*s)/2,(h-page.getWidth()*s)/2);}page.render(bitmap,null,matrix,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_PRINT);int[] row=new int[w];for(int y=0;y<h;y++){bitmap.getPixels(row,0,w,0,y,w,1);writeRasterRow(out,row,gray);}}finally{bitmap.recycle();}
 }}
 }}finally{source.delete();}
 }
 static void photoPdf(InputStream input,java.io.File target,String media,int margin,String scaling)throws IOException{photoPdf(input,target,media,margin,scaling,false);}
 static void photoPdf(InputStream input,java.io.File target,String media,int margin,String scaling,boolean landscape)throws IOException{photoPdf(input,target,media,margin,scaling,landscape,0);}
 static void photoPdf(InputStream input,java.io.File target,String media,int margin,String scaling,boolean landscape,int rotation)throws IOException{
 java.io.File source=java.io.File.createTempFile("photo-source-",".jpg",cacheDirectory);
 android.graphics.Bitmap bitmap;
 try{try(OutputStream out=new FileOutputStream(source)){byte[] buffer=new byte[16384];int n;while((n=input.read(buffer))!=-1)out.write(buffer,0,n);}
 if(android.os.Build.VERSION.SDK_INT>=28){bitmap=android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(source),(decoder,header,src)->{int iw=header.getSize().getWidth(),ih=header.getSize().getHeight();double factor=Math.min(1.0,Math.min(4200.0/Math.max(iw,ih),Math.sqrt(12000000.0/((double)iw*ih))));decoder.setTargetSize(Math.max(1,(int)(iw*factor)),Math.max(1,(int)(ih*factor)));decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);});}
 else{android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeFile(source.getPath(),options);if(options.outWidth<=0||options.outHeight<=0)throw new IOException("Foto tidak valid");int sample=1;while(Math.max(options.outWidth,options.outHeight)/(double)sample>4200||((double)options.outWidth*options.outHeight)/(sample*(double)sample)>12000000)sample*=2;options.inJustDecodeBounds=false;options.inSampleSize=sample;bitmap=android.graphics.BitmapFactory.decodeFile(source.getPath(),options);}
 }finally{source.delete();}
 if(bitmap==null)throw new IOException("Foto tidak dapat dibaca");
 if(rotation!=0){android.graphics.Matrix rotate=new android.graphics.Matrix();rotate.setRotate(rotation);android.graphics.Bitmap rotated=android.graphics.Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),rotate,true);if(rotated!=bitmap)bitmap.recycle();bitmap=rotated;}
 int w=595,h=842;if(media.equals("custom_10rmini_210x252mm"))h=714;else if(media.equals("custom_f4_210x330mm"))h=935;else if(media.equals("na_legal_8.5x14in")){w=612;h=1008;}else if(media.equals("na_index-4x6_4x6in")){w=288;h=432;}
 if(landscape){int swap=w;w=h;h=swap;}
 float m=(margin<0?5:margin)*72f/25.4f;float aw=w-2*m,ah=h-2*m;float s="fill".equals(scaling)?Math.max(aw/bitmap.getWidth(),ah/bitmap.getHeight()):Math.min(aw/bitmap.getWidth(),ah/bitmap.getHeight());float bw=bitmap.getWidth()*s,bh=bitmap.getHeight()*s;
 android.graphics.pdf.PdfDocument pdf=new android.graphics.pdf.PdfDocument();try{android.graphics.pdf.PdfDocument.Page page=pdf.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(w,h,1).create());android.graphics.Canvas canvas=page.getCanvas();canvas.drawColor(android.graphics.Color.WHITE);canvas.save();canvas.clipRect(m,m,w-m,h-m);canvas.drawBitmap(bitmap,null,new android.graphics.RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG));canvas.restore();pdf.finishPage(page);try(OutputStream out=new FileOutputStream(target)){pdf.writeTo(out);}}finally{try{pdf.close();}finally{bitmap.recycle();}}
 }
 static void attr(DataOutputStream d,int tag,String name,String value)throws IOException{byte[] n=name.getBytes(StandardCharsets.UTF_8),v=value.getBytes(StandardCharsets.UTF_8);d.writeByte(tag);d.writeShort(n.length);d.write(n);d.writeShort(v.length);d.write(v);}
 static String probe(String uri)throws Exception {return query(uri);}
 static String query(String uri)throws Exception {URI u=printerUri(uri);URL url=new URI("ipps".equals(u.getScheme())?"https":"http",null,u.getHost(),u.getPort()<0?631:u.getPort(),u.getPath().isEmpty()?"/ipp/print":u.getPath(),null,null).toURL(); ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(bytes);d.writeShort(0x0101);d.writeShort(0x000b);d.writeInt(1);d.writeByte(1);attr(d,0x47,"attributes-charset","utf-8");attr(d,0x48,"attributes-natural-language","en");attr(d,0x45,"printer-uri",uri);attr(d,0x44,"requested-attributes","all");d.writeByte(3);HttpURLConnection c=printerConnection(url);try{c.setInstanceFollowRedirects(false);c.setConnectTimeout(5000);c.setReadTimeout(10000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/ipp");c.setRequestProperty("Accept","application/ipp");try(OutputStream out=c.getOutputStream()){out.write(bytes.toByteArray());}checkResponse(c);try(DataInputStream in=new DataInputStream(c.getInputStream())){int version=in.readUnsignedShort();if(version!=0x0100&&version!=0x0101&&version!=0x0200&&version!=0x0201&&version!=0x0202)throw new IOException("Versi respons IPP tidak valid: "+Integer.toHexString(version));int status=in.readUnsignedShort();if(in.readInt()!=1)throw new IOException("Request ID salah");if(status>=0x100)throw new IOException("IPP status "+Integer.toHexString(status));return "Printer merespons IPP.\n"+readCapabilities(in);}}finally{c.disconnect();}}
 static String readCapabilities(DataInputStream in)throws IOException{
 java.util.Map<String,java.util.List<String>> values=new java.util.LinkedHashMap<>();
 String name="";int depth=0;
 for(int count=0;count<20000;count++){
 int tag=in.readUnsignedByte();if(tag==3)break;if(tag<16){name="";continue;}
 int nl=in.readUnsignedShort();byte[] nb=new byte[nl];in.readFully(nb);if(nl>0)name=new String(nb,StandardCharsets.UTF_8);
 int vl=in.readUnsignedShort();byte[] v=new byte[vl];in.readFully(v);
 if(tag==0x34){depth++;continue;}if(tag==0x37){depth=Math.max(0,depth-1);continue;}if(depth>0)continue;
 if(!java.util.Arrays.asList("printer-name","printer-make-and-model","document-format-supported","media-supported","media-type-supported","print-color-mode-supported","print-quality-supported","orientation-requested-supported","printer-state","printer-state-reasons","printer-is-accepting-jobs","queued-job-count","copies-supported","operations-supported","page-ranges-supported","print-scaling-supported","media-col-supported","media-top-margin-supported","media-bottom-margin-supported","media-left-margin-supported","media-right-margin-supported","pwg-raster-document-type-supported","pwg-raster-document-resolution-supported","printer-resolution-supported","printer-resolution-default").contains(name))continue;
 String val;
 if((tag==0x21||tag==0x23)&&vl==4){int n=java.nio.ByteBuffer.wrap(v).getInt();val=name.equals("print-quality-supported")?(n==3?"Draft":n==4?"Normal":n==5?"High":String.valueOf(n)):String.valueOf(n);}
 else if(tag==0x32&&vl==9){java.nio.ByteBuffer resolution=java.nio.ByteBuffer.wrap(v);int x=resolution.getInt(),y=resolution.getInt(),unit=resolution.get()&255;val=x+"x"+y+(unit==3?" dpi":unit==4?" dpcm":" (unknown unit)");}
 else if(tag==0x33&&vl==8){java.nio.ByteBuffer range=java.nio.ByteBuffer.wrap(v);val=range.getInt()+"-"+range.getInt();}
 else if(tag==0x22&&vl==1)val=v[0]==0?"false":"true";
 else if(tag>=0x40&&tag<=0x4f)val=new String(v,StandardCharsets.UTF_8);else continue;
 values.computeIfAbsent(name,k->new java.util.ArrayList<>()).add(val);
 }
 StringBuilder b=new StringBuilder();for(java.util.Map.Entry<String,java.util.List<String>> e:values.entrySet())b.append(e.getKey()).append(": ").append(android.text.TextUtils.join(", ",e.getValue())).append("\n");
 return b.length()==0?"Tidak ada atribut yang dapat ditampilkan.":b.toString();
 }
 static void integer(DataOutputStream d,int tag,String name,int value)throws IOException{byte[] n=name.getBytes(StandardCharsets.UTF_8);d.writeByte(tag);d.writeShort(n.length);d.write(n);d.writeShort(4);d.writeInt(value);}
 static String print(String uri,String mime,InputStream document,String media,int quality,String color,String type,int copies)throws Exception{return print(uri,mime,document,media,quality,color,type,copies,-1,"");}
 static String print(String uri,String mime,InputStream document,String media,int quality,String color,String type,int copies,int margin,String scaling)throws Exception{return print(uri,mime,document,media,quality,color,type,copies,margin,scaling,0,0);}
 static String printPhotoLayout(String uri,InputStream document,String media,int quality,String color,String type,int copies,boolean landscape,String scaling,int rotation)throws Exception{
 String report=query(uri);File pdf=File.createTempFile("layout-",".pdf",cacheDirectory);File raster=null;
 try{photoPdf(document,pdf,media,-1,scaling,landscape,rotation);raster=File.createTempFile("layout-",".ras",cacheDirectory);try(InputStream in=new FileInputStream(pdf)){rasterPdf(in,raster,report,media,0,0,color,false,landscape,copies);}try(InputStream in=new FileInputStream(raster)){return printInternal(uri,"image/pwg-raster",in,media,quality,color,type,1,-1,"none",0,0,3);}}finally{pdf.delete();if(raster!=null)raster.delete();}
 }
 static String printOriented(String uri,String mime,InputStream document,String media,int quality,String color,String type,int copies,int first,int last,boolean landscape)throws Exception{
 if(copies>1&&"image/jpeg".equals(mime))return printPhotoLayout(uri,document,media,quality,color,type,copies,landscape,"fit",0);
 if(!landscape&&!(copies>1&&"application/pdf".equals(mime)))return printInternal(uri,mime,document,media,quality,color,type,copies,-1,"",first,last,3);
 String report=query(uri);File raster=File.createTempFile("landscape-",".ras",cacheDirectory);File pdf=null;
 try{if("image/jpeg".equals(mime)){pdf=File.createTempFile("landscape-",".pdf",cacheDirectory);photoPdf(document,pdf,media,-1,"",true);try(InputStream in=new FileInputStream(pdf)){rasterPdf(in,raster,report,media,0,0,color,false,true);}}else if("application/pdf".equals(mime)){rasterPdf(document,raster,report,media,first,last,color,false,landscape,copies);}else throw new IOException("Format landscape tidak didukung");
 try(InputStream in=new FileInputStream(raster)){return printInternal(uri,"image/pwg-raster",in,media,quality,color,type,1,-1,"none",0,0,3);}
 }finally{raster.delete();if(pdf!=null)pdf.delete();}
 }
 static String print(String uri,String mime,InputStream document,String media,int quality,String color,String type,int copies,int margin,String scaling,int first,int last)throws Exception{
 return printInternal(uri,mime,document,media,quality,color,type,copies,margin,scaling,first,last,0);
 }
 static String printInternal(String uri,String mime,InputStream document,String media,int quality,String color,String type,int copies,int margin,String scaling,int first,int last,int orientation)throws Exception{
 if(copies<1||copies>99)throw new IOException("Jumlah salinan harus 1–99");
 if(copies>1){
 File repeat=File.createTempFile("copies-",".bin",cacheDirectory);
 try{try(OutputStream out=new FileOutputStream(repeat)){byte[] buf=new byte[65536];int n;while((n=document.read(buf))!=-1)if(n>0)out.write(buf,0,n);}
 int accepted=0;for(int i=0;i<copies;i++){try(InputStream in=new FileInputStream(repeat)){printInternal(uri,mime,in,media,quality,color,type,1,margin,scaling,first,last,orientation);accepted++;}catch(Exception e){throw new IOException("Salinan diterima: "+accepted+"/"+copies+". Salinan berikutnya gagal atau belum terkonfirmasi: "+e.getMessage()+". Jangan ulang seluruh pekerjaan sebelum memeriksa printer.",e);}}
 return copies+" pekerjaan salinan diterima printer. Periksa hasil fisik.";
 }finally{repeat.delete();}
 }
 if(first<0||last<first||(first==0&&last!=0))throw new IOException("Rentang halaman tidak valid");
 String capabilities=query(uri);
 if("false".equals(capability(capabilities,"printer-is-accepting-jobs")))throw new IOException("Printer sedang tidak menerima pekerjaan cetak");
 String copyRange=capability(capabilities,"copies-supported");if(copyRange.matches("[0-9]+-[0-9]+")){String[] bounds=copyRange.split("-");if(copies<Integer.parseInt(bounds[0])||copies>Integer.parseInt(bounds[1]))throw new IOException("Jumlah salinan tidak didukung: "+copyRange);}
 requireSupported(capabilities,"print-color-mode-supported",color,"Mode warna");
 requireSupported(capabilities,"media-type-supported",type,"Jenis kertas");
 requireSupported(capabilities,"print-quality-supported",quality==0?"":quality==3?"Draft":quality==4?"Normal":quality==5?"High":String.valueOf(quality),"Kualitas");
 String formats=null;for(String line:capabilities.split("\\n")){if(line.startsWith("document-format-supported: "))formats=line.substring("document-format-supported: ".length());}if(formats!=null&&!java.util.Arrays.asList(formats.split(", ")).contains(mime)){
 if("image/jpeg".equals(mime)&&java.util.Arrays.asList(formats.split(", ")).contains("application/pdf")&&!"none".equals(scaling)&&cacheDirectory!=null){
 java.io.File temp=java.io.File.createTempFile("photo-print-",".pdf",cacheDirectory);
 try{photoPdf(document,temp,media,margin,scaling);try(InputStream pdf=new FileInputStream(temp)){return printInternal(uri,"application/pdf",pdf,media,quality,color,type,copies,margin,"",0,0,orientation)+" (JPEG dikonversi ke PDF)";}}finally{temp.delete();}
 }
 if("image/jpeg".equals(mime)&&java.util.Arrays.asList(formats.split(", ")).contains("image/pwg-raster")&&cacheDirectory!=null&&margin<0&&!"none".equals(scaling)){
 File pdfTemp=File.createTempFile("photo-raster-",".pdf",cacheDirectory);
 try{photoPdf(document,pdfTemp,media,margin,scaling);File rasterTemp=File.createTempFile("photo-raster-",".ras",cacheDirectory);
 try{try(InputStream pdf=new FileInputStream(pdfTemp)){rasterPdf(pdf,rasterTemp,capabilities,media,0,0,color,true);}
 try(InputStream raster=new FileInputStream(rasterTemp)){return printInternal(uri,"image/pwg-raster",raster,media,quality,color,type,copies,margin,"none",0,0,orientation)+" (JPEG ke PWG Raster)";}
 }finally{rasterTemp.delete();}
 }finally{pdfTemp.delete();}
 }
 if("application/pdf".equals(mime)&&java.util.Arrays.asList(formats.split(", ")).contains("image/pwg-raster")&&cacheDirectory!=null&&margin<0&&(scaling.isEmpty()||"fit".equals(scaling)||"none".equals(scaling))){
 File temp=File.createTempFile("pwg-print-",".ras",cacheDirectory);try{rasterPdf(document,temp,capabilities,media,first,last,color,"none".equals(scaling));try(InputStream raster=new FileInputStream(temp)){return printInternal(uri,"image/pwg-raster",raster,media,quality,color,type,copies,margin,"none".equals(scaling)?"none":"",0,0,orientation)+" (PDF ke PWG Raster)";}}finally{temp.delete();}
 }
 throw new IOException("Printer tidak mendukung format "+mime+". Format tersedia: "+formats);
 }
 if(first>0&&"false".equals(capability(capabilities,"page-ranges-supported")))throw new IOException("Printer tidak mendukung pilihan halaman untuk format ini");
 requireSupported(capabilities,"print-scaling-supported",scaling,"Skala cetak");
 if(margin>=0)for(String side:new String[]{"top","bottom","left","right"})requireSupported(capabilities,"media-"+side+"-margin-supported",String.valueOf(margin*100),"Margin "+side);
 URI u=printerUri(uri);
 URL url=new URI("ipps".equals(u.getScheme())?"https":"http",null,u.getHost(),u.getPort()<0?631:u.getPort(),u.getPath().isEmpty()?"/ipp/print":u.getPath(),null,null).toURL();
 ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(bytes);
 d.writeShort(0x0101);d.writeShort(2);d.writeInt(2);d.writeByte(1);attr(d,0x47,"attributes-charset","utf-8");attr(d,0x48,"attributes-natural-language","en");attr(d,0x45,"printer-uri",uri);attr(d,0x49,"document-format",mime);d.writeByte(0x22);d.writeShort("ipp-attribute-fidelity".length());d.writeBytes("ipp-attribute-fidelity");d.writeShort(1);d.writeByte(1);
 d.writeByte(2);
 if(orientation!=0){requireSupported(capabilities,"orientation-requested-supported",String.valueOf(orientation),"Orientasi");integer(d,0x23,"orientation-requested",orientation);}
 if(media.equals("custom_10rmini_210x252mm")||media.equals("custom_f4_210x330mm")||margin>=0){
 int w=21000,h=29700;if(media.equals("custom_10rmini_210x252mm"))h=25200;else if(media.equals("custom_f4_210x330mm"))h=33000;else if(media.equals("na_legal_8.5x14in")){w=21590;h=35560;}else if(media.equals("na_index-4x6_4x6in")){w=10160;h=15240;}
 attr(d,0x34,"media-col","");attr(d,0x4a,"","media-size");attr(d,0x34,"","");
 attr(d,0x4a,"","x-dimension");integer(d,0x21,"",w);
 attr(d,0x4a,"","y-dimension");integer(d,0x21,"",h);
 attr(d,0x37,"","");if(!type.isEmpty()){attr(d,0x4a,"","media-type");attr(d,0x44,"",type);}if(margin>=0){for(String side:new String[]{"top","bottom","left","right"}){attr(d,0x4a,"","media-"+side+"-margin");integer(d,0x21,"",margin*100);}}attr(d,0x37,"","");
 }else{attr(d,0x44,"media",media);if(!type.isEmpty())attr(d,0x44,"media-type",type);}
 if(first>0){d.writeByte(0x33);d.writeShort(11);d.writeBytes("page-ranges");d.writeShort(8);d.writeInt(first);d.writeInt(last);}if(!scaling.isEmpty())attr(d,0x44,"print-scaling",scaling);if(quality!=0)integer(d,0x23,"print-quality",quality);if(!color.isEmpty())attr(d,0x44,"print-color-mode",color);if(copies>1)integer(d,0x21,"copies",copies);d.writeByte(3);
 if(cacheDirectory==null)throw new IOException("Cache aplikasi belum siap");
 File spool=File.createTempFile("ipp-job-",".bin",cacheDirectory);
 try{
 try(OutputStream out=new FileOutputStream(spool)){out.write(bytes.toByteArray());byte[] buf=new byte[65536];int n;while((n=document.read(buf))!=-1){if(n>0)out.write(buf,0,n);}}
 HttpURLConnection c=printerConnection(url);try{c.setInstanceFollowRedirects(false);c.setConnectTimeout(5000);c.setReadTimeout(120000);c.setRequestMethod("POST");c.setDoOutput(true);c.setFixedLengthStreamingMode(spool.length());c.setRequestProperty("Content-Type","application/ipp");c.setRequestProperty("Accept","application/ipp");
 try(InputStream payload=new FileInputStream(spool);OutputStream out=c.getOutputStream()){byte[] buf=new byte[65536];int n;while((n=payload.read(buf))!=-1)out.write(buf,0,n);}
 checkResponse(c);
 try(DataInputStream in=new DataInputStream(c.getInputStream())){int version=in.readUnsignedShort();if(version!=0x0100&&version!=0x0101&&version!=0x0200&&version!=0x0201&&version!=0x0202)throw new IOException("Versi respons cetak IPP tidak valid: "+Integer.toHexString(version));int status=in.readUnsignedShort();if(in.readInt()!=2)throw new IOException("Request ID salah");if(status>=0x100)throw new IOException("IPP status "+Integer.toHexString(status)+(status==0x040b?" (atribut atau nilai cetak tidak didukung)":"")+"; periksa printer sebelum mencoba lagi");return status==0?"Job diterima printer. Periksa hasil fisik.":"Job diterima dengan peringatan IPP "+Integer.toHexString(status)+". Periksa hasil; jangan langsung cetak ulang.";}
 }finally{c.disconnect();}
 }finally{spool.delete();}
 }

}
