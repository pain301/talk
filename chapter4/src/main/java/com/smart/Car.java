package com.smart;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class Car implements BeanFactoryAware, BeanNameAware, InitializingBean, DisposableBean {
    private String brand;
    private String color;
    private int maxSpeed;
    private BeanFactory beanFactory;
    private String beanName;

    public Car() {
        System.out.println("Car#Car()");
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        System.out.println("Car#setBrand(): " + brand);
        this.brand = brand;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(int maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public String toString() {
        return "brand:" + brand + "/color:" + color + "/maxSpeed:"+ maxSpeed;
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        System.out.println("Car implement BeanFactoryAware#setBeanFactory()");
        this.beanFactory = beanFactory;
    }

    public void setBeanName(String beanName) {
        System.out.println("Car implement BeanNameAware#setBeanName(), beanName: " + beanName);
        this.beanName = beanName;
    }

    public void afterPropertiesSet() throws Exception {
        System.out.println("Car implement InitializingBean#afterPropertiesSet()");
    }

    public void destroy() throws Exception {
        System.out.println("Car implement DisposableBean#destory()");
    }

    public void init() {
        System.out.println("Car#init()");
        this.maxSpeed = 240;
    }

    public void destory() {
        System.out.println("Car#destroy()");
    }

}
