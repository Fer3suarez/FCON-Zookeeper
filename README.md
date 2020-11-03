Laboratorio Zookeeper para la asignatura FCON

Implementado un cerrojo distribuido para la elección del lider de un cluster de Zookeeper.

Lo primero que se debe hacer es arrancar un ensemble Zookeeper, para ello se debe ejecutar el siguiente comando en el directorio adecuado:
```
./zkServer.sh start ~/zookeeper/standalone_zk.cfg
```
Se supone que en directorio home, hay creada una carpeta llamada zookeeper, en la que dentro está el fichero de configuración standalone_zk.cfg.
Una vez arrancado el ensemble, se deben configurar las variables de entorno para la ejecución del programa.

- Configurar la variable de entorno CLASSPATH. Este variable permite al programa Java a buscar las bibliotecas necesarias de los programas que se van a ejecutar:

  ```
  export CLASSPATH=$CLASSPATH:˜/zookeeper/apache-zookeeper-3.6.2-bin/lib/*
  ```

- Configurar la variable de entorno PATH. Esta variable permite ejecutar directamente los programas de ZooKeeper

  ```
  export PATH=$PATH:˜/zookeeper/apache-zookeeper-3.6.2-bin/bin
  ```
- Configurar la variable de entorno para la ejecución de programas java en una terminal
  
  ```
  export CLASSPATH=$CLASSPATH:˜/zookeeper/lab zookeeper.jar
  ```

A partir de estos pasos, se puede empezar con la creación de znodes, para ver el funcionamiento del cerrojo distribuido, así como visualizar en todo momento quién es el lider del cluster.
