package restclient;

import java.io.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
 
public class FilePost {
    private File[] files;
    private String[] filenames;
    private Boolean[] isMainFileToExecute;
    public int SIZE;
    /**
     * Constructor method
     * @param size the initial size of the arrays
     */
    public FilePost (int size){
        SIZE = size;
        this.files = new File[SIZE];
        this.filenames = new String[SIZE];
        this.isMainFileToExecute = new Boolean[SIZE];
        for(int i=0;i<SIZE;i++){
            this.files[i]=null;
            this.filenames[i]="";
            this.isMainFileToExecute[i]=false;
        }
    }
    
    /**
     * Method that increases the arrays' size
     */
    private void IncreaseArraySize(){
        SIZE = SIZE*2;
        File[] newfiles = new File[SIZE];
        String[] newfilenames = new String[SIZE];
        Boolean[] newisMainFileToExecute = new Boolean[SIZE];
        
        int i = 0;
        for(;i<SIZE/2;i++){
            newfiles[i] = this.files[i];
            newfilenames[i] = this.filenames[i];
            newisMainFileToExecute[i] = this.isMainFileToExecute[i];
        }
        for(;i<SIZE;i++){
            newfiles[i] = null;
            newfilenames[i] = "";
            newisMainFileToExecute[i] = false;
        }
        
        this.files = newfiles;
        this.filenames = newfilenames;
        this.isMainFileToExecute = newisMainFileToExecute;
    }
    /**
     * Method that adds a file so that it will be sent
     * @param data the file that the user want to add to the to-be-sent queue
     * @param name name of the file
     * @param isMainFileToExecute true if this file is the main template file
     * @return true if the file gets successfully added, else false
     */
    public boolean addFile(File data,String name,Boolean isMainFileToExecute){
        int i = 0;
        for(;i<SIZE;i++){
            try{
                if(!files[i].isFile()){
                    break;
                }
            }
            catch(NullPointerException e)
            {
                break;
            }
        }
        if(i==SIZE){
            this.IncreaseArraySize();
        }
        this.files[i] = data;
        this.filenames[i] = name;
        this.isMainFileToExecute[i] = isMainFileToExecute;
        return true;
    }
    
    /**
     * A method to execute an Http Request and construct a response object
     * @param requestBase the request that needs to be executed
     * @return server response as <code>String</code>
     */
    private static String executeRequest(HttpRequestBase requestBase){
        String responseString = "" ;
        System.out.println(requestBase.getRequestLine().toString());
        InputStream responseStream = null ;
        HttpClient client = new DefaultHttpClient () ;
        try{
            HttpResponse response = client.execute(requestBase) ;
            if (response != null){
                HttpEntity responseEntity = response.getEntity() ;
 
                if (responseEntity != null){
                    responseStream = responseEntity.getContent() ;
                    if (responseStream != null){
                        BufferedReader br = new BufferedReader (new InputStreamReader (responseStream)) ;
                        String responseLine = br.readLine() ;
                        String tempResponseString = "" ;
                        while (responseLine != null){
                            tempResponseString = tempResponseString + responseLine + System.getProperty("line.separator") ;
                            responseLine = br.readLine() ;
                        }
                        br.close() ;
                        if (tempResponseString.length() > 0){
                            responseString = tempResponseString ;
                        }
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            if (responseStream != null){
                try {
                    responseStream.close() ;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        client.getConnectionManager().shutdown() ;
 
        return responseString ;
    }
    /**
     * Method that builds the multi-part form data request
     * @param urlString the urlString to which the file needs to be uploaded
     * @return server response as <code>String</code>
     */
    public String executeMultiPartRequest(String urlString) {
 
        HttpPost postRequest = new HttpPost (urlString) ;
        FileBody fileBody;
        StringBody stringBody;
        try{
            MultipartEntity multiPartEntity = new MultipartEntity () ;
            int i=0,x=1;
            for(;i<SIZE;i++){
                if(this.isMainFileToExecute[i]){
                    break;
                }
            }
            fileBody = new FileBody(this.files[i], "application/msword") ;
            multiPartEntity.addPart("template", fileBody);
            for(int j=0;j<SIZE;j++){
                if (i==j) continue; //when i equals j the file is the template
                                    //file we already added in the previous step
                if (this.filenames[j].isEmpty()) continue; //that means that there
                                                           //is no file on j 
                stringBody = new StringBody(this.filenames[j]);
                multiPartEntity.addPart("files.file"+x++, stringBody);
                fileBody = new FileBody(this.files[j], "application/octect-stream") ;
                multiPartEntity.addPart(this.filenames[j], fileBody);
            }
            postRequest.setEntity(multiPartEntity) ;
        }catch (/*UnsupportedEncoding*/Exception ex){
            ex.printStackTrace() ;
        }
 
        return executeRequest (postRequest) ;
    }
 
    
    public static void main(String args[]){
        FilePost fileUpload = new FilePost(2);
        fileUpload.addFile(new File("file_in.doc"),"template",true);
        fileUpload.addFile(new File("Sunset.jpg"),"image1.jpg",false);
        String response = fileUpload.executeMultiPartRequest("http://localhost:8080/printservice/print/word") ;
        System.out.println("Response : "+response);
    }
 
}