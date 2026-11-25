package com.iingstd.photoprint;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;
final class Ipp {
 static java.io.File cacheDirectory;
 static void photoPdf(InputStream input,java.io.File target,String media,int margin,String scaling)throws IOException{
 java.io.File source=java.io.File.createTempFile("photo-source-",".jpg",cacheDirectory);
 android.graphics.Bitmap bitmap;
 try{try(OutputStream out=new FileOutputStream(source)){byte[] buffer=new byte[16384];int n;while((n=input.read(buffer))!=-1)out.write(buffer,0,n);}
 if(android.os.Build.VERSION.SDK_INT>=28){bitmap=android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(source),(decoder,header,src)->{int iw=header.getSize().getWidth(),ih=header.getSize().getHeight();double factor=Math.min(1.0,Math.min(4200.0/Math.max(iw,ih),Math.sqrt(12000000.0/((double)iw*ih))));decoder.setTargetSize(Math.max(1,(int)(iw*factor)),Math.max(1,(int)(ih*factor)));decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);});}
 else{android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeFile(source.getPath(),options);if(options.outWidth<=0||options.outHeight<=0)throw new IOException("Foto tidak valid");int sample=1;while(Math.max(options.outWidth,options.outHeight)/(double)sample>4200||((double)options.outWidth*options.outHeight)/(sample*(double)sample)>12000000)sample*=2;options.inJustDecodeBounds=false;options.inSampleSize=sample;bitmap=android.graphics.BitmapFactory.decodeFile(source.getPath(),options);}
 }finally{source.delete();}
 if(bitmap==null)throw new IOException("Foto tidak dapat dibaca");
 int w=595,h=842;if(media.equals("custom_f4_210x330mm"))h=935;else if(media.equals("na_legal_8.5x14in")){w=612;h=1008;}else if(media.equals("na_index-4x6_4x6in")){w=288;h=432;}
 float m=(margin<0?5:margin)*72f/25.4f;float aw=w-2*m,ah=h-2*m;float s="fill".equals(scaling)?Math.max(aw/bitmap.getWidth(),ah/bitmap.getHeight()):Math.min(aw/bitmap.getWidth(),ah/bitmap.getHeight());float bw=bitmap.getWidth()*s,bh=bitmap.getHeight()*s;
 android.graphics.pdf.PdfDocument pdf=new android.graphics.pdf.PdfDocument();try{android.graphics.pdf.PdfDocument.Page page=pdf.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(w,h,1).create());android.graphics.Canvas canvas=page.getCanvas();canvas.drawColor(android.graphics.Color.WHITE);canvas.save();canvas.clipRect(m,m,w-m,h-m);canvas.drawBitmap(bitmap,null,new android.graphics.RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG));canvas.restore();pdf.finishPage(page);try(OutputStream out=new FileOutputStream(target)){pdf.writeTo(out);}}finally{try{pdf.close();}finally{bitmap.recycle();}}
 }
 static void attr(DataOutputStream d,int tag,String name,String value)throws IOException{byte[] n=name.getBytes(StandardCharsets.UTF_8),v=value.getBytes(StandardCharsets.UTF_8);d.writeByte(tag);d.writeShort(n.length);d.write(n);d.writeShort(v.length);d.write(v);}
 static String probe(String uri)throws Exception {return query(uri);}
 static String query(String uri)throws Exception {URI u=new URI(uri);if(!"ipp".equals(u.getScheme())||u.getHost()==null)throw new IOException("Gunakan alamat ipp:// printer");URL url=new URI("http",null,u.getHost(),u.getPort()<0?631:u.getPort(),u.getPath().isEmpty()?"/ipp/print":u.getPath(),null,null).toURL(); ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(bytes);d.writeShort(0x0101);d.writeShort(0x000b);d.writeInt(1);d.writeByte(1);attr(d,0x47,"attributes-charset","utf-8");attr(d,0x48,"attributes-natural-language","en");attr(d,0x45,"printer-uri",uri);attr(d,0x44,"requested-attributes","all");d.writeByte(3);HttpURLConnection c=(HttpURLConnection)url.openConnection();try{c.setInstanceFollowRedirects(false);c.setConnectTimeout(5000);c.setReadTimeout(10000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/ipp");try(OutputStream out=c.getOutputStream()){out.write(bytes.toByteArray());}if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());try(DataInputStream in=new DataInputStream(c.getInputStream())){in.readUnsignedShort();int status=in.readUnsignedShort();if(in.readInt()!=1)throw new IOException("Request ID salah");if(status>=0x100)throw new IOException("IPP status "+Integer.toHexString(status));return "Printer merespons IPP.\n"+readCapabilities(in);}}finally{c.disconnect();}}
 static String readCapabilities(DataInputStream in)throws IOException{
 java.util.Map<String,java.util.List<String>> values=new java.util.LinkedHashMap<>();
 String name="";int depth=0;
 for(int count=0;count<20000;count++){
 int tag=in.readUnsignedByte();if(tag==3)break;if(tag<16){name="";continue;}
 int nl=in.readUnsignedShort();byte[] nb=new byte[nl];in.readFully(nb);if(nl>0)name=new String(nb,StandardCharsets.UTF_8);
 int vl=in.readUnsignedShort();byte[] v=new byte[vl];in.readFully(v);
 if(tag==0x34){depth++;continue;}if(tag==0x37){depth=Math.max(0,depth-1);continue;}if(depth>0)continue;
 if(!java.util.Arrays.asList("printer-name","printer-make-and-model","document-format-supported","media-supported","media-type-supported","print-color-mode-supported","print-quality-supported","printer-state","printer-state-reasons","operations-supported","page-ranges-supported","print-scaling-supported","media-col-supported","media-top-margin-supported","media-bottom-margin-supported","media-left-margin-supported","media-right-margin-supported").contains(name))continue;
 String val;
 if((tag==0x21||tag==0x23)&&vl==4){int n=java.nio.ByteBuffer.wrap(v).getInt();val=name.equals("print-quality-supported")?(n==3?"Draft":n==4?"Normal":n==5?"High":String.valueOf(n)):String.valueOf(n);}
 else if(tag>=0x40&&tag<=0x4f)val=new String(v,StandardCharsets.UTF_8);else continue;
 values.computeIfAbsent(name,k->new java.util.ArrayList<>()).add(val);
 }
 StringBuilder b=new StringBuilder();for(java.util.Map.Entry<String,java.util.List<String>> e:values.entrySet())b.append(e.getKey()).append(": ").append(android.text.TextUtils.join(", ",e.getValue())).append("\n");
 return b.length()==0?"Tidak ada atribut yang dapat ditampilkan.":b.toString();
 }
 static void integer(DataOutputStream d,int tag,String name,int value)throws IOException{byte[] n=name.getBytes(StandardCharsets.UTF_8);d.writeByte(tag);d.writeShort(n.length);d.write(n);d.writeShort(4);d.writeInt(value);}
 static String print(String uri,String mime,InputStream document,String media,int quality,String color,String type,int copies)throws Exception{return print(uri,mime,document,media,quality,color,type,copies,-1,"");}
 static String print(String uri,String mime,InputStream document,String media,int quality,String color,String type,int copies,int margin,String scaling)throws Exception{return print(uri,mime,document,media,quality,color,type,copies,margin,scaling,0,0);}
 static String print(String uri,String mime,InputStream document,String media,int quality,String color,String type,int copies,int margin,String scaling,int first,int last)throws Exception{
 String capabilities=query(uri);String formats=null;for(String line:capabilities.split("\\n")){if(line.startsWith("document-format-supported: "))formats=line.substring("document-format-supported: ".length());}if(formats!=null&&!java.util.Arrays.asList(formats.split(", ")).contains(mime)){
 if("image/jpeg".equals(mime)&&java.util.Arrays.asList(formats.split(", ")).contains("application/pdf")&&!"none".equals(scaling)&&cacheDirectory!=null){
 java.io.File temp=java.io.File.createTempFile("photo-print-",".pdf",cacheDirectory);
 try{photoPdf(document,temp,media,margin,scaling);try(InputStream pdf=new FileInputStream(temp)){return print(uri,"application/pdf",pdf,media,quality,color,type,copies,margin,"",0,0)+" (JPEG dikonversi ke PDF)";}}finally{temp.delete();}
 }
 throw new IOException("Printer tidak mendukung format "+mime+". Format tersedia: "+formats);
 }
 URI u=new URI(uri);if(!"ipp".equals(u.getScheme())||u.getHost()==null)throw new IOException("Alamat IPP tidak valid");
 URL url=new URI("http",null,u.getHost(),u.getPort()<0?631:u.getPort(),u.getPath().isEmpty()?"/ipp/print":u.getPath(),null,null).toURL();
 ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(bytes);
 d.writeShort(0x0101);d.writeShort(2);d.writeInt(2);d.writeByte(1);attr(d,0x47,"attributes-charset","utf-8");attr(d,0x48,"attributes-natural-language","en");attr(d,0x45,"printer-uri",uri);attr(d,0x49,"document-format",mime);d.writeByte(0x22);d.writeShort("ipp-attribute-fidelity".length());d.writeBytes("ipp-attribute-fidelity");d.writeShort(1);d.writeByte(1);
 d.writeByte(2);
 if(media.equals("custom_f4_210x330mm")||margin>=0){
 int w=21000,h=29700;if(media.equals("custom_f4_210x330mm"))h=33000;else if(media.equals("na_legal_8.5x14in")){w=21590;h=35560;}else if(media.equals("na_index-4x6_4x6in")){w=10160;h=15240;}
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
 HttpURLConnection c=(HttpURLConnection)url.openConnection();try{c.setInstanceFollowRedirects(false);c.setConnectTimeout(5000);c.setReadTimeout(120000);c.setRequestMethod("POST");c.setDoOutput(true);c.setFixedLengthStreamingMode(spool.length());c.setRequestProperty("Content-Type","application/ipp");
 try(InputStream payload=new FileInputStream(spool);OutputStream out=c.getOutputStream()){byte[] buf=new byte[65536];int n;while((n=payload.read(buf))!=-1)out.write(buf,0,n);}
 if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());
 try(DataInputStream in=new DataInputStream(c.getInputStream())){in.readUnsignedShort();int status=in.readUnsignedShort();if(in.readInt()!=2)throw new IOException("Request ID salah");if(status!=0)throw new IOException("IPP status "+Integer.toHexString(status)+"; periksa printer sebelum mencoba lagi");return "Job diterima printer. Periksa hasil fisik.";}
 }finally{c.disconnect();}
 }finally{spool.delete();}
 }

}
