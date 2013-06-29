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

   public static boolean isPositiveInteger( String input )  
   {  
      try  
      {  
         int i = Integer.parseInt( input );

         if(i > 0)
         {
            return true;  
         }
         else
         {
            return false;
         }
      }  
      catch( Exception e)  
      {  
         return false;
      }  
   }
}
