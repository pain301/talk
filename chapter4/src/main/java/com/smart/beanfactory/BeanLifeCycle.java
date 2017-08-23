package com.smart.beanfactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.smart.Car;

import java.beans.PropertyDescriptor;

public class BeanLifeCycle {

    public static void lifeCycle() {
        Resource res = new ClassPathResource("com/smart/beanfactory/beans.xml");

        BeanFactory bf= new DefaultListableBeanFactory();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader((DefaultListableBeanFactory)bf);
        reader.loadBeanDefinitions(res);

        ((DefaultListableBeanFactory) bf).addBeanPostProcessor(new MyBeanPostProcessor());
        ((DefaultListableBeanFactory) bf).addBeanPostProcessor(new MyInstantiationAwareBeanPostProcessor());

        Car car1 = (Car) bf.getBean("car");
        car1.setColor("blue");
        Car car2 = (Car) bf.getBean("car");
        System.out.println(car1 == car2);

        ((DefaultListableBeanFactory) bf).destroySingletons();
    }

    public static void life() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:com/smart/beanfactory/beans.xml");
//        Car car1 = (Car) ctx.getBean("car");
//        car1.setColor("blue");
//        Car car2 = (Car) ctx.getBean("car");
//        System.out.println(car1 == car2);
    }

	public static void main(String[] args) {
//		lifeCycle();
		life();
	}
}

class MyBeanPostProcessor implements BeanPostProcessor {

    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
        if (s.equals("car")) {
            System.out.println("post process car before init by BeanPostProcessor");
        }
        return o;
    }

    public Object postProcessAfterInitialization(Object o, String s) throws BeansException {

        if (s.equals("car")) {
            System.out.println("post process car after init by BeanPostProcessor");
        }
        return o;
    }
}

class MyInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if ("car".equals(beanName)) {
            System.out.println("post process car before init by InstantiationAwareBeanPostProcessorAdapter");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if ("car".equals(beanName)) {
            System.out.println("post process car after init by InstantiationAwareBeanPostProcessorAdapter");
        }
        return bean;
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        if ("car".equals(beanName)) {
            System.out.println("post process car before instantiation");
        }
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        if ("car".equals(beanName)) {
            System.out.println("post process car after instantiation");
        }
        return true;
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {
        if ("car".equals(beanName)) {
            System.out.println("post process car property values");
        }
        return pvs;
    }
}

class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        BeanDefinition beanDefinition = configurableListableBeanFactory.getBeanDefinition("car");
        beanDefinition.getPropertyValues().addPropertyValue("color", "yellow");
        System.out.println("BeanFactoryPostProcessor#postProcessBeanFactory()");
    }
}