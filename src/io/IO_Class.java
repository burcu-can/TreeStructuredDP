package io;

import java.io.File;
import java.io.FileOutputStream;

import java.io.IOException;
import java.io.RandomAccessFile;

public class IO_Class{

	public void CloseFile(RandomAccessFile raf) {
		// TODO Auto-generated method stub
		try{
			raf.close();
			
		}
		catch (IOException e) {
			System.out.println("IOException:");
			e.printStackTrace();
		}
	}

	public RandomAccessFile OpenFile(String fileName) {
		// TODO Auto-generated method stub
		try{
			File file = new File(fileName);
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			return raf;
			}
		
		catch (IOException e) {
			System.out.println("Unable to open "+ fileName +": " + e.getMessage());
			System.out.println("IOException:");
			e.printStackTrace();
	        }
		return null;
	}

	public String ReadLine(RandomAccessFile raf) {
		// TODO Auto-generated method stub
		try{
			return raf.readLine();
		}
		catch (IOException e) {
			System.out.println("IOException:");
			e.printStackTrace();
		}
		return null;
	}

	public void WriteString(String str, RandomAccessFile raf) {
		// TODO Auto-generated method stub
		try{
			raf.writeBytes(str);
		}
		catch (IOException e) {
			System.out.println("IOException:");
			e.printStackTrace();
		}
	}

	public void WriteFloat(Float f, RandomAccessFile raf) {
		// TODO Auto-generated method stub
		try{
			raf.writeFloat(f);
		}
		catch (IOException e) {
			System.out.println("IOException:");
			e.printStackTrace();
		}
	}
	
	public void ClearContents(String fileName){
		try{
			FileOutputStream erasor = new FileOutputStream(fileName);
			erasor.write((new String("")).getBytes());
			erasor.close();
		}
		catch (IOException e) {
			System.out.println("IOException:");
			e.printStackTrace();
		}
	}
}
