package com.iingstd.photoprint;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;
final class Ipp {
 static void attr(DataOutputStream d,int tag,String name,String value)throws IOException{byte[] n=name.getBytes(StandardCharsets.UTF_8),v=value.getBytes(StandardCharsets.UTF_8);d.writeByte(tag);d.writeShort(n.length);d.write(n);d.writeShort(v.length);d.write(v);}
 static String probe(String uri)throws Exception {URI u=new URI(uri);if(!"ipp".equals(u.getScheme())||u.getHost()==null)throw new IOException("Gunakan alamat ipp:// printer");URL url=new URI("http",null,u.getHost(),u.getPort()<0?631:u.getPort(),u.getPath().isEmpty()?"/ipp/print":u.getPath(),null,null).toURL(); ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(bytes);d.writeShort(0x0101);d.writeShort(0x000b);d.writeInt(1);d.writeByte(1);attr(d,0x47,"attributes-charset","utf-8");attr(d,0x48,"attributes-natural-language","en");attr(d,0x45,"printer-uri",uri);attr(d,0x44,"requested-attributes","all");d.writeByte(3);HttpURLConnection c=(HttpURLConnection)url.openConnection();try{c.setInstanceFollowRedirects(false);c.setConnectTimeout(5000);c.setReadTimeout(10000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/ipp");try(OutputStream out=c.getOutputStream()){out.write(bytes.toByteArray());}if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());try(DataInputStream in=new DataInputStream(c.getInputStream())){in.readUnsignedShort();int status=in.readUnsignedShort();if(in.readInt()!=1)throw new IOException("Request ID salah");if(status>=0x100)throw new IOException("IPP status "+Integer.toHexString(status));return "Printer merespons IPP.\n"+readCapabilities(in);}}finally{c.disconnect();}}
 static String readCapabilities(DataInputStream in)throws IOException{
 java.util.Map<String,java.util.List<String>> values=new java.util.LinkedHashMap<>();
 String name="";int depth=0;
 for(int count=0;count<20000;count++){
 int tag=in.readUnsignedByte();if(tag==3)break;if(tag<16){name="";continue;}
 int nl=in.readUnsignedShort();byte[] nb=new byte[nl];in.readFully(nb);if(nl>0)name=new String(nb,StandardCharsets.UTF_8);
 int vl=in.readUnsignedShort();byte[] v=new byte[vl];in.readFully(v);
 if(tag==0x34){depth++;continue;}if(tag==0x37){depth=Math.max(0,depth-1);continue;}if(depth>0)continue;
 if(!java.util.Arrays.asList("printer-name","printer-make-and-model","document-format-supported","media-supported","media-type-supported","print-color-mode-supported","print-quality-supported","printer-state","printer-state-reasons","operations-supported","print-scaling-supported","media-col-supported","media-top-margin-supported","media-bottom-margin-supported","media-left-margin-supported","media-right-margin-supported").contains(name))continue;
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
 static String print(String uri,String mime,InputStream document,String media,int quality,String color,String type,int copies,int margin,String scaling)throws Exception{
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
 if(!scaling.isEmpty())attr(d,0x44,"print-scaling",scaling);if(quality!=0)integer(d,0x23,"print-quality",quality);if(!color.isEmpty())attr(d,0x44,"print-color-mode",color);if(copies>1)integer(d,0x21,"copies",copies);d.writeByte(3);
 HttpURLConnection c=(HttpURLConnection)url.openConnection();try{c.setInstanceFollowRedirects(false);c.setConnectTimeout(5000);c.setReadTimeout(60000);c.setRequestMethod("POST");c.setDoOutput(true);c.setChunkedStreamingMode(16384);c.setRequestProperty("Content-Type","application/ipp");
 try(OutputStream out=c.getOutputStream()){out.write(bytes.toByteArray());byte[] buf=new byte[16384];int n;while((n=document.read(buf))!=-1)out.write(buf,0,n);}
 if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());
 try(DataInputStream in=new DataInputStream(c.getInputStream())){in.readUnsignedShort();int status=in.readUnsignedShort();if(in.readInt()!=2)throw new IOException("Request ID salah");if(status!=0)throw new IOException("IPP status "+Integer.toHexString(status)+"; periksa printer sebelum mencoba lagi");return "Job diterima printer. Periksa hasil fisik.";}
 }finally{c.disconnect();}
 }

}
