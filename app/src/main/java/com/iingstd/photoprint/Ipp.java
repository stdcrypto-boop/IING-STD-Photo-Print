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
 if(!java.util.Arrays.asList("printer-name","printer-make-and-model","document-format-supported","media-supported","media-type-supported","print-color-mode-supported","print-quality-supported","printer-state","printer-state-reasons","operations-supported").contains(name))continue;
 String val;
 if((tag==0x21||tag==0x23)&&vl==4){int n=java.nio.ByteBuffer.wrap(v).getInt();val=name.equals("print-quality-supported")?(n==3?"Draft":n==4?"Normal":n==5?"High":String.valueOf(n)):String.valueOf(n);}
 else if(tag>=0x40&&tag<=0x4f)val=new String(v,StandardCharsets.UTF_8);else continue;
 values.computeIfAbsent(name,k->new java.util.ArrayList<>()).add(val);
 }
 StringBuilder b=new StringBuilder();for(java.util.Map.Entry<String,java.util.List<String>> e:values.entrySet())b.append(e.getKey()).append(": ").append(android.text.TextUtils.join(", ",e.getValue())).append("\n");
 return b.length()==0?"Tidak ada atribut yang dapat ditampilkan.":b.toString();
 }
 static String print(String uri,String mime,InputStream document,String media)throws Exception{
 URI u=new URI(uri);if(!"ipp".equals(u.getScheme())||u.getHost()==null)throw new IOException("Alamat IPP tidak valid");
 URL url=new URI("http",null,u.getHost(),u.getPort()<0?631:u.getPort(),u.getPath().isEmpty()?"/ipp/print":u.getPath(),null,null).toURL();
 ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(bytes);
 d.writeShort(0x0101);d.writeShort(2);d.writeInt(2);d.writeByte(1);attr(d,0x47,"attributes-charset","utf-8");attr(d,0x48,"attributes-natural-language","en");attr(d,0x45,"printer-uri",uri);attr(d,0x49,"document-format",mime);d.writeByte(0x22);d.writeShort(22);d.writeBytes("ipp-attribute-fidelity");d.writeShort(1);d.writeByte(1);
 d.writeByte(2);attr(d,0x44,"media",media);d.writeByte(3);
 HttpURLConnection c=(HttpURLConnection)url.openConnection();try{c.setInstanceFollowRedirects(false);c.setConnectTimeout(5000);c.setReadTimeout(60000);c.setRequestMethod("POST");c.setDoOutput(true);c.setChunkedStreamingMode(16384);c.setRequestProperty("Content-Type","application/ipp");
 try(OutputStream out=c.getOutputStream()){out.write(bytes.toByteArray());byte[] buf=new byte[16384];int n;while((n=document.read(buf))!=-1)out.write(buf,0,n);}
 if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());
 try(DataInputStream in=new DataInputStream(c.getInputStream())){in.readUnsignedShort();int status=in.readUnsignedShort();if(in.readInt()!=2)throw new IOException("Request ID salah");if(status!=0)throw new IOException("IPP status "+Integer.toHexString(status)+"; periksa printer sebelum mencoba lagi");return "Job diterima printer. Periksa hasil fisik.";}
 }finally{c.disconnect();}
 }

}
