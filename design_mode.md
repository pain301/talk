```java
abstract class Strategy {
  public abstract void algorithm();
}

class ConcreteStrategyA extends Strategy {
  public void algorithm() {}
}

class ConcreteStrategyB extends Strategy {
  public void algorithm() {}
}

class Context {
  Strategy strategy;

  public Context(Strategy strategy) {
    this.strategy = strategy;
  }

  // SimpleFactory + Strategy
  public Context(String type) {
    switch (type) {
      case "A":
        strategy = new ConcreteStrategyA();
        break;
      case "B":
        strategy = new ConcreteStrategyB();
        break;
    }
  }

  public void contextInterface() {
    strategy.algorithm();
  }
}
```

```java
abstract class Product {}

class ProductA extends Product {}

class ProductB extends Product {}

interface IFactory {
  Product createProduct();
}

class FactoryA implements IFactory {
  Product createProduct() {
    return new ProductA();
  }
}

class FactoryB implements IFactory {
  Product createProduct() {
    return new ProductB();
  }
}
```

```java
abstract class Component {
  public abstract void operate();
}

class ConcreteComponent extends Component {
  public void operate() {}
}

abstract class DecoratorComponent extends Component {
  protected Component component;

  public void setComponent(Component component) {
    this.component = component;
  }

  public void operate() {
    if (component != null) {
      component.operate();
    }
  }
}

class ConcreteDecoratorComponentA extends DecorateComponent {
  public void operate() {
    super.operate();
  }
}

class ConcreteDecoratorComponentB extends DecorateComponent {
  public void operate() {
    super.operate();
  }
}
```

```java
abstract class Subject {
  public abstract void operate();
}

class RealSubject extends Subject {
  public void operate() {}
}

class Proxy extends Subject {
  private Subject subject;

  public void operate() {
    if (subject != null) {
      subject.operate();
    }
  }
}
```

```java
abstract class AbstractClass {
  protected abstract void operate();

  public void template() {
    operate();
  }
}

class ConcreteClass extends AbstractClass {
  protected void operate() {}
}
```

```java
class SubSystemA {
  public void operateA() {}
}

class SubSystemB {
  public void operateB() {}
}

class Facade {
  private SubSystemA ssa;
  private SubSystemB ssb;

  Facade() {
    ssa = new SubSystemA();
    ssb = new SubSystemB();
  }

  public void operate() {
    ssa.operateA();
    ssb.operateB();
  }
}
```

```java
abstract class Builder {
  public abstract void buildPartA();
  public abstract void buildPartB();
  public abstract Product completeBuild();
}

class ConcreteBuilderA extends Builder {
  private Product product = new Product();

  public void buildPartA() {}
  public void buildPartB() {}

  public Product completeBuild() {
    return product;
  }
}

class ConcreteBuilderB extends Builder {
  private Product product = new Product();

  public void buildPartA() {}
  public void buildPartB() {}

  public Product completeBuild() {
    return product;
  }
}

class Director {
  public Product buildProduct(Builder builder) {
    builder.buildPartA();
    builder.buildPartB();
    return builder.completeBuild();
  }
}
```

```java
abstract class Subject {
  private List<Observer> observers = new ArrayList<Observer>();

  public void attach(Observer observer) {
    observers.add(observer);
  }

  public void detach(Observer observer) {
    observers.remove(observer);
  }

  public void notify() {
    foreach (Observer observer : observers) {
      observer.update();
    }
  }
}

abstract class Observer {
  public abstract void update();
}

class ConcreteSubject extends Subject {
  private String state;
}

class ConcreteObserver extends Observer {
  private Subject subject;
  private String name;
  private String state;

  ConcreteObserver(String name, Subject subject) {
    this.name = name;
    this.subject = subject;
  }

  public void update() {
    this.state = subject.state;
  }
}
```

```java
abstract class State {
  public abstract void handle(Context context);
}

class ConcreteStateA extends State {
  public void handle(Context context) {
    context.state = new ConcreteStateB();
    // context.request();
  }
}

class ConcreteStateB extends State {
  public void handle(Context context) {
    context.state = new ConcreteStateC();
    // context.request();
  }
}

class Context {
  private State state;

  public Context(State state) {
    this.state = state;
  }

  public void request() {
    state.handle(this);
  }
}
```
