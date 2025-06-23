.class public Suma
.super java/lang/Object

.method public <init>()V
   aload_0
   invokespecial java/lang/Object/<init>()V
   return
.end method

.method public static main([Ljava/lang/String;)V
   .limit stack 8
   .limit locals 4
   ldc 5
   istore_1
   ldc 7
   istore_2
   iload_1
   iload_2
   iadd
   istore_3
   getstatic java/lang/System/out Ljava/io/PrintStream;
   iload_3
   invokevirtual java/io/PrintStream/println(I)V
   return
.end method
