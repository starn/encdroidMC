package de.aflx.sardine.impl.handler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jdom2.*;
import org.jdom2.input.*;



public class XmlMapper {

	
	public XmlMapper(String xml, Object rootObject){
	      this(new ByteArrayInputStream(xml.getBytes()),rootObject);
	}
	
	
	public XmlMapper(InputStream xmlIS, Object rootObject){
		SAXBuilder sxb = new SAXBuilder();
	      try
	      {
	         //On crée un nouveau document JDOM avec en argument le fichier XML
	         //Le parsing est terminé ;)
	    	  Document document = document = sxb.build(xmlIS);
	    	  Element root = document.getRootElement();
	    	  fillObjectRecursive(rootObject,root);
	      }
	      catch(Exception e){
	    	  throw new RuntimeException(e);
	      }
	}	
	
	
	private void fillObjectRecursive(Object object, Element element){
//		String name = element.getName();
//		String content = element.getText();
		
		List<Element> children = element.getChildren();
		//Method[] methods = object.getClass().getMethods();
		Field[] fields = object.getClass().getDeclaredFields();
		
		
		
		for (Field f: fields){
//			if (m.getName().equals(getSetterName(name))){
//				try {
//					m.invoke(object, content);
//				} catch (Exception e){
//					e.printStackTrace();
//				}
//			}
			
			for (Element child: children){
				
				if (f.getName().equals( child.getName())){
					//Class[] paramsType = m.getParameterTypes();
					Class fieldType = f.getType();
					
					
					
//					Method setterToInvoke = null;
//					try {
//						setterToInvoke = object.getClass().getMethod(setter, fieldType);
//					} catch (NoSuchMethodException e){
//						System.err.println("Warning: the field "+f.getName()+" in bean "+object.getClass().getName()+ " has no setter. this field cannot be filled and is ignored" );
//						continue;
//					}
						
					try {						
						//if (paramsType[0]==String.class){
						if (fieldType==String.class){
							//getSetter(object.getClass(), f) .invoke(object, child.getText());
							invokeSetter(object,f,child.getText());
							
							
						} else if (fieldType == List.class){
							//get existing list
							Method getter = null;
							try {
								getter = object.getClass().getMethod(getgetterName(child.getName()));
							} catch (NoSuchMethodException e){
								System.err.println("Warning: the field "+f.getName()+" in bean "+object.getClass().getName()+ " has no getter. this field cannot be filled and is ignored" );
								continue;
							}
							List childObject = (List)getter.invoke(object );
							if (childObject==null){
								childObject = new ArrayList();
								//setterToInvoke.invoke(object,childObject);
								invokeSetter(object,f,childObject);
							}
							
							//get list generic type
							if (!(f.getGenericType() instanceof ParameterizedType)) {
								System.err.println("Warning: a list "+f.getName()+" in bean "+object.getClass().getName()+ " has no generic declaration. this field cannot be filled and is ignored" );
								continue;
							}
						    ParameterizedType listType = (ParameterizedType) f.getGenericType();
						    Class<?> listGenericClass = (Class<?>) listType.getActualTypeArguments()[0];
						 	if (listGenericClass == String.class){
						 		childObject.add(child.getText());
						 	} else {
								Constructor constructor = listGenericClass.getConstructor(new Class[0]);
								Object listElement = constructor.newInstance(new Object[0]);
								fillObjectRecursive(listElement,child);
								childObject.add(listElement);
								//setterToInvoke.invoke(object, listObject);
								//fillObjectRecursive(listObject,child);
						 		
						 	}
							
						 	
						 	
						} else {
							Constructor constructor = fieldType.getConstructor(new Class[0]);
							Object childObject = constructor.newInstance(new Object[0]);
							//setterToInvoke.invoke(object, childObject);
							invokeSetter(object,f,childObject);
							fillObjectRecursive(childObject,child);
						}
					} catch (Exception e){
						e.printStackTrace();
					}
				}
			}			
		}
				//(getSetterName(name), new Class[]{String.class})

		
		

	}
	
	private String getSetterName(String attributeName){
		return "set"+attributeName.substring(0,1).toUpperCase()+attributeName.substring(1);
	}
	
	private String getgetterName(String attributeName){
		return "get"+attributeName.substring(0,1).toUpperCase()+attributeName.substring(1);
	}
	
	private void invokeSetter(Object object,Field f,Object value){
		String fieldName=f.getName();
		Class setterParam = f.getType();
		String setter = getSetterName(fieldName);
		Method setterToInvoke = null;
		try {
			setterToInvoke = object.getClass().getMethod(setter, setterParam);
		} catch (NoSuchMethodException e){
			System.err.println("Warning: the field "+fieldName+" in bean "+object.getClass().getName()+ " has no setter. this field cannot be filled and is ignored" );
			return;
		}
		try {
			setterToInvoke.invoke(object, value);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
}
