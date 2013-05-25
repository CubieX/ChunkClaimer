package com.github.CubieX.ChunkClaimer;

public final class CCUtil
{
   private CCUtil()
   {
      // static class
   }
   
   public static boolean isInteger( String input )  
   {  
      try  
      {  
         Integer.parseInt( input );  
         return true;  
      }  
      catch( Exception e)  
      {  
         return false;  
      }  
   }  
}
