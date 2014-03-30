package org.mkosem.datamapper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.mkosem.datamapper.DataMapper.DataMapping;
import org.mkosem.datamapper.testobjects.AllPrimitiveClass;
import org.mkosem.datamapper.testobjects.AllWrappedClass;
import org.mkosem.datamapper.testobjects.IDataModelObject;

public class DataMapperTest {
	
	private static Float testFloat = 1.1F;
	private static Boolean testBoolean = true;
	private static Long testLong = 1000L;
	private static Integer testInt = 10;
	private static Byte testByte = (byte) 0;
	private static Byte[] testBytes = new Byte[]{(byte) 0, (byte) 1, (byte) 2};
	private static Short testShort = 1;
	private static Character testChar = 'a';
	private static String testString = "test";
	private static final String[] methodNames = new String[] {"TestFloat", "TestBoolean", "TestLong", "TestInt", "TestByte", "TestBytes", "TestShort", "TestChar", "TestString"};
	private static final Object[] methodParameters = new Object[] {testFloat, testBoolean, testLong, testInt, testByte, testBytes, testShort, testChar, testString};
	
	public static void main(String[] args) {
		DataMapperTest mapperClass = new DataMapperTest();
		mapperClass.testAllWrapped();
		mapperClass.testAllPrimitive();
		mapperClass.testSetAllWrapped();
		mapperClass.testSetAllPrimitive();
	}
	
	
	public void testAllPrimitive() {
		System.out.println("primitive");
		DataMapper dataMapper = new DataMapper();
		DataMapping mapping = dataMapper.getMapping(AllPrimitiveClass.class, methodNames, methodParameters);
		for (int i = 0 ; i < methodNames.length ; i++) {
			System.out.println(mapping.getSetterForField(methodNames[i], methodParameters[i]));
		}
	}
	
	public void testAllWrapped() {
		System.out.println("wrapped");
		DataMapper dataMapper = new DataMapper();
		DataMapping mapping = dataMapper.getMapping(AllWrappedClass.class, methodNames, methodParameters);
		for (int i = 0 ; i < methodNames.length ; i++) {
			System.out.println(mapping.getSetterForField(methodNames[i], methodParameters[i]));
		}
	}
	
	public void testSetAllPrimitive() {
		System.out.println("primitive");
		
		DataMapper dataMapper = new DataMapper();
		List<IDataModelObject> wrappedClassResults = dataMapper.mapValuesToObject(AllPrimitiveClass.class, methodNames, new Object[][]{methodParameters});
		AllPrimitiveClass primitiveClass = (AllPrimitiveClass) wrappedClassResults.get(0);
		System.out.println(primitiveClass.getTestBoolean());
		System.out.println(primitiveClass.getTestChar());
		System.out.println(primitiveClass.getTestFloat());
		System.out.println(primitiveClass.getTestInt());
		System.out.println(primitiveClass.getTestLong());
		System.out.println(primitiveClass.getTestString());
		System.out.println(primitiveClass.getTestByte());
		System.out.println(primitiveClass.getTestBytes());
		System.out.println(primitiveClass.getTestShort());
		
		ExecutorService executor = Executors.newFixedThreadPool(256);
		long startTime = System.currentTimeMillis();
		for (int i = 0 ; i< 10000000 ; i++) {
			executor.submit(new Runnable(){
				@Override
				public void run() {
					try {
						DataMapper dataMapper = new DataMapper();
						List<IDataModelObject> wrappedClassResults = dataMapper.mapValuesToObject(AllPrimitiveClass.class, methodNames, new Object[][]{methodParameters});
						wrappedClassResults.get(0);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}});
		}
		executor.shutdown();
		try {
			executor.awaitTermination(30, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Execution time in ms: " + (System.currentTimeMillis() - startTime));
	}
	
	public void testSetAllWrapped() {
		System.out.println("wrapped");
		DataMapper dataMapper = new DataMapper();
		List<IDataModelObject> wrappedClassResults = dataMapper.mapValuesToObject(AllWrappedClass.class, methodNames, new Object[][]{methodParameters});
		AllWrappedClass wrappedClass = (AllWrappedClass) wrappedClassResults.get(0);
		System.out.println(wrappedClass.getTestBoolean());
		System.out.println(wrappedClass.getTestChar());
		System.out.println(wrappedClass.getTestFloat());
		System.out.println(wrappedClass.getTestInt());
		System.out.println(wrappedClass.getTestLong());
		System.out.println(wrappedClass.getTestString());
		System.out.println(wrappedClass.getTestByte());
		System.out.println(wrappedClass.getTestBytes());
		System.out.println(wrappedClass.getTestShort());
	}
	
}
