package com.company;

import java.io.*; 
import java.net.*; 
import java.util.Scanner; 
  
// Client class 
public class FileMerger  
{ 
    final String file1;
    final String file2;
    final String file;
    public FileMerger(String file1,String file2,String file)
    {
        this.file1 = file1;
        this.file2 = file2;
        this.file = file;
    }
    
    public void merge() throws FileNotFoundException, IOException
    {
     
            File myFile1 = new File(this.file1);
            File myFile2 = new File(this.file2);
            byte[] mybytearray1 = new byte[(int) myFile1.length()];
            byte[] mybytearray2 = new byte[(int) myFile2.length()];
            FileInputStream fis = new FileInputStream(myFile1);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(mybytearray1, 0, mybytearray1.length);

            fis = new FileInputStream(myFile2);
            bis = new BufferedInputStream(fis);
            bis.read(mybytearray2, 0, mybytearray2.length);
            
            File myFile = new File(this.file);
            byte[] mybytearray = new byte[(int) (myFile1.length()+myFile2.length())];
            for(int i=0;i<mybytearray1.length;i++)
            {
                mybytearray[i] = mybytearray1[i];
            }
            int offset = mybytearray1.length;
            for(int i=0;i<mybytearray2.length;i++)
            {
                mybytearray[offset+i] = mybytearray2[i];
            }
            FileOutputStream fos = new FileOutputStream(myFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(mybytearray, 0, mybytearray.length);
            bos.flush();
            
    }
} 