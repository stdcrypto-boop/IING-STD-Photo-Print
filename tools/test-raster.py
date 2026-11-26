import pathlib, subprocess, tempfile
source=pathlib.Path("app/src/main/java/com/iingstd/photoprint/Ipp.java").read_text()
methods=source[source.index(" static int rasterPixel("):source.index(" static void rasterPdf(")]
harness="""import java.io.*;import java.util.*;
class RasterFixture {
"""+methods+"""
static void check(int[] row,boolean gray)throws Exception{
 ByteArrayOutputStream b=new ByteArrayOutputStream();writeRasterRow(new DataOutputStream(b),row,gray);
 DataInputStream in=new DataInputStream(new ByteArrayInputStream(b.toByteArray()));
 if(in.readUnsignedByte()!=0)throw new AssertionError("row repeat");
 int x=0;
 while(x<row.length){
 int tag=in.readUnsignedByte();if(tag==128)throw new AssertionError("unexpected marker");
 int n=tag<128?tag+1:257-tag;
 if(x+n>row.length)throw new AssertionError("overflow");
 int pixel=0;
 for(int j=0;j<n;j++){
 if(j==0||tag>128){pixel=in.readUnsignedByte();if(!gray)pixel=(pixel<<16)|(in.readUnsignedByte()<<8)|in.readUnsignedByte();}
 int p=row[x++],expected=gray?((p>>>16&255)*299+(p>>>8&255)*587+(p&255)*114+500)/1000:p&0xffffff;
 if(pixel!=expected)throw new AssertionError("pixel mismatch");
 }}
 if(in.read()!=-1)throw new AssertionError("trailing data");
}
public static void main(String[] args)throws Exception{
 for(boolean gray:new boolean[]{false,true})for(int dpi:new int[]{150,200,300})for(int[] paper:new int[][]{{21000,29700},{21000,33000},{21590,35560},{10160,15240}}){
 int w=(int)Math.round(paper[0]*dpi/2540.0),h=(int)Math.round(paper[1]*dpi/2540.0);
 byte[] bytes=rasterHeader(w,h,dpi,paper[0],paper[1],gray,3);
 if(bytes.length!=1796||!new String(bytes,0,9,java.nio.charset.StandardCharsets.US_ASCII).equals("PwgRaster"))throw new AssertionError("header signature");
 java.nio.ByteBuffer bb=java.nio.ByteBuffer.wrap(bytes);
 int[][] fields={{276,dpi},{280,dpi},{372,w},{376,h},{384,8},{388,gray?8:24},{392,w*(gray?1:3)},{396,0},{400,gray?18:19},{420,gray?1:3},{452,3},{456,1},{460,1},{472,w},{476,h}};
 for(int[] field:fields)if(bb.getInt(field[0])!=field[1])throw new AssertionError("header field "+field[0]);
 }
 Random random=new Random(42);
 for(boolean gray:new boolean[]{false,true})for(int n:new int[]{1,2,127,128,129,255,256,257,2480}){
 int[] row=new int[n];check(row,gray);
 Arrays.fill(row,0xffffff);check(row,gray);
 for(int i=0;i<n;i++)row[i]=random.nextInt();check(row,gray);
 for(int i=0;i<n;i++)row[i]=(i/3)%2==0?0xff0000:0x00ff00;check(row,gray);
 }
 System.out.println("Raster row fixtures passed");
}
}
"""
with tempfile.TemporaryDirectory() as d:
 p=pathlib.Path(d)/"RasterFixture.java"
 p.write_text(harness)
 subprocess.run(["java",str(p)],check=True)

# Exercise production layout methods with a minimal RectF substitute.
source=pathlib.Path("app/src/main/java/com/iingstd/photoprint/MainActivity.java").read_text()
methods=source[source.index(" static float[] paperMm("):source.index(" InputStream passportInput(")].replace("android.graphics.RectF","RectF")
harness="""import java.io.*;
class LayoutFixture {
static class RectF {float left,top,right,bottom;RectF(float a,float b,float c,float d){left=a;top=b;right=c;bottom=d;}}
"""+methods+"""
static void require(boolean b){if(!b)throw new AssertionError("layout geometry");}
public static void main(String[] args)throws Exception{
String[] media={"iso_a4_210x297mm","custom_f4_210x330mm","na_legal_8.5x14in","na_index-4x6_4x6in"};
for(String paper:media)for(int size=1;size<=3;size++)for(int gap:new int[]{0,2,3,5,10})for(int margin:new int[]{-1,0,5,10}){
float[] dimensions=paperMm(paper);int w=new int[]{0,20,30,40}[size],h=new int[]{0,30,40,60}[size],m=Math.max(5,margin);
int cols=(int)((dimensions[0]-2*m+gap)/(w+gap)),rows=(int)((dimensions[1]-2*m+gap)/(h+gap));
RectF[] cells=passportCells(size,paper,margin,cols*rows,gap);
require(cells.length==cols*rows);
for(int i=0;i<cells.length;i++){RectF c=cells[i];require(c.left>=m&&c.top>=m&&c.right<=dimensions[0]-m+0.001&&c.bottom<=dimensions[1]-m+0.001);require(c.right-c.left==w&&c.bottom-c.top==h);if(i%cols>0)require(c.left-cells[i-1].right==gap);if(i>=cols)require(c.top-cells[i-cols].bottom==gap);}
try{passportCells(size,paper,margin,cols*rows+1,gap);throw new AssertionError("overflow accepted");}catch(IOException expected){}
}
System.out.println("Passport layout fixtures passed");
}
}
"""
with tempfile.TemporaryDirectory() as d:
 p=pathlib.Path(d)/"LayoutFixture.java"
 p.write_text(harness)
 subprocess.run(["java",str(p)],check=True)

methods=source[source.index(" static int[] cropBounds("):source.index(" static android.graphics.Bitmap editBitmap(")]
harness="""class CropFixture {
"""+methods+"""
public static void main(String[] args){
for(int w:new int[]{1,2,99,100,1920})for(int h:new int[]{1,2,101,100,1080})for(int crop=0;crop<=6;crop++)for(int x:new int[]{0,50,100})for(int y:new int[]{0,50,100}){
int[] b=cropBounds(w,h,crop,x,y);
if(b[0]<0||b[1]<0||b[2]<1||b[3]<1||b[0]+b[2]>w||b[1]+b[3]>h)throw new AssertionError("crop bounds");
if(x==0&&b[0]!=0||y==0&&b[1]!=0||x==100&&b[0]+b[2]!=w||y==100&&b[1]+b[3]!=h)throw new AssertionError("anchor");
if(crop==0&&(b[2]!=w||b[3]!=h))throw new AssertionError("original");
if(crop!=0){double ratio=new double[]{0,1,2.0/3,0.75,2.0/3,4.0/3,1.5}[crop];if(b[2]!=w&&b[3]!=h||Math.abs(b[2]-b[3]*ratio)>Math.max(1,ratio))throw new AssertionError("aspect");}
}
System.out.println("Crop fixtures passed");
}
}
"""
with tempfile.TemporaryDirectory() as d:
 p=pathlib.Path(d)/"CropFixture.java"
 p.write_text(harness)
 subprocess.run(["java",str(p)],check=True)

source=pathlib.Path("app/src/main/java/com/iingstd/photoprint/Ipp.java").read_text()
methods=source[source.index(" static String capability("):source.index(" static int rasterPixel(")]
harness="""import java.io.*;
class CapabilityFixture {
"""+methods+"""
static void reject(String report,String key,String value)throws Exception{
try{requireSupported(report,key,value,"Setting");throw new AssertionError("unsupported accepted: "+value);}catch(IOException expected){if(!expected.getMessage().contains(value))throw new AssertionError("missing diagnostic");}
}
public static void main(String[] args)throws Exception{
String report="print-color-mode-supported: color, monochrome\\nmedia-type-supported: stationery, photographic-glossy\\nprint-quality-supported: Draft, Normal, High\\nprint-scaling-supported: fit, none\\n";
String[][] accepted={{"print-color-mode-supported","color"},{"print-color-mode-supported","monochrome"},{"media-type-supported","stationery"},{"media-type-supported","photographic-glossy"},{"print-quality-supported","Draft"},{"print-quality-supported","Normal"},{"print-quality-supported","High"},{"print-scaling-supported","fit"},{"print-scaling-supported","none"},{"print-scaling-supported",""},{"missing-attribute","unknown"}};
for(String[] a:accepted)requireSupported(report,a[0],a[1],"Setting");
reject(report,"print-color-mode-supported","auto");
reject(report,"media-type-supported","photographic-matte");
reject(report,"print-quality-supported","6");
reject(report,"print-scaling-supported","fill");
reject(report,"print-scaling-supported","fi");
requireSupported("","print-scaling-supported","none","Setting");
if(!capability(report,"print-quality-supported").equals("Draft, Normal, High"))throw new AssertionError("capability parsing");
System.out.println("Capability fixtures passed");
}
}
"""
with tempfile.TemporaryDirectory() as d:
 p=pathlib.Path(d)/"CapabilityFixture.java"
 p.write_text(harness)
 subprocess.run(["java",str(p)],check=True)

methods=source[source.index(" static URI printerUri("):source.index(" static String capability(")]
harness="""import java.io.*;import java.net.*;
class TransportFixture {
"""+methods+"""
static void reject(String value)throws Exception{try{printerUri(value);throw new AssertionError("accepted "+value);}catch(IOException expected){}}
static class Response extends HttpURLConnection{
final int code;final String type;
Response(int code,String type)throws Exception{super(new URL("http://localhost"));this.code=code;this.type=type;}
public int getResponseCode(){return code;}public String getContentType(){return type;}
public void connect(){}public void disconnect(){}public boolean usingProxy(){return false;}
}
static void rejectResponse(int code,String type)throws Exception{try{checkResponse(new Response(code,type));throw new AssertionError("accepted invalid response");}catch(IOException expected){}}
public static void main(String[] args)throws Exception{
for(String uri:new String[]{"ipp://192.168.1.1:631/ipp/print","ipp://printer.local","ipp://[::1]:631/ipp/print"})printerUri(uri);
for(String uri:new String[]{"http://printer.local","ipp:///print","ipp://user@printer.local","ipp://printer.local#x","ipp://printer.local?x=1","ipp://printer.local:0","ipp://printer.local:65536","ipp://bad host"})reject(uri);
checkResponse(new Response(200,"application/ipp"));checkResponse(new Response(200,"Application/IPP; charset=binary"));
rejectResponse(302,"application/ipp");rejectResponse(500,"application/ipp");rejectResponse(200,"text/html");checkResponse(new Response(200,null));checkResponse(new Response(200,"application/octet-stream"));
System.out.println("Transport fixtures passed");
}
}
"""
with tempfile.TemporaryDirectory() as d:
 p=pathlib.Path(d)/"TransportFixture.java"
 p.write_text(harness)
 subprocess.run(["java",str(p)],check=True)
