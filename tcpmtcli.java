import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class tcpmtcli {
    public static void main(String[] args) throws IOException {

        //Se comprueba si los argumentos de entrada son los correctos
        if (args.length != 2) {
            System.out.println("Sintaxis incorrecta. Uso correcto: tcpmtcli <direccion_ip> <puerto>");
            System.out.println("Ejemplo: tcpmtcli 127.0.0.1 12345");
            return;
        }

        //Se obtiene la dirección del servidor a partir del argumento indicado por línea de comandos
        InetAddress serverAddress = InetAddress.getByName(args[0]);

        // Se obtiene el puerto del servidor por la línea de comandos
        int serverPort = Integer.parseInt(args[1]);

        // Creamos el Socket
        try (Socket socket = new Socket();) {
            socket.connect(new InetSocketAddress(serverAddress, serverPort), 15000);
            socket.setSoTimeout(15000);

            // Crear streams de entrada y salida
            InputStream entrada = socket.getInputStream();
            OutputStream salida = socket.getOutputStream();

            // Bucle de comunicación del servidor y puerto
            while (true) {

                // Solicitar operación al usuario
                System.out.print("Introduzca operacion o QUIT (por ejemplo 2 * -3, -4 + 5, -6 !): ");
                String operacionLinea = new BufferedReader(new InputStreamReader(System.in)).readLine();

                // Salir si el usuario introduce QUIT
                if ("QUIT".equals(operacionLinea)) {
                    break;
                }

                // Dividimos por partes la operación
                //String[] partes = operacionLinea.split("[\\+\\-\\*\\/\\%\\!]");
                String[] partes = operacionLinea.split(" ");
                Arrays.stream(partes).filter(part -> !part.isEmpty()).toArray(String[]::new);

                int operando1 = Integer.parseInt(partes[0]);
                int operando2 = (partes.length == 3) ? Integer.parseInt(partes[2]) : 0;

                int simbolo;
                int longitud;

                // if (operacionLinea.contains("+")) {
                //     simbolo = 1;
                //     longitud = 2;
                // } else if (operacionLinea.contains("-")) {
                //     simbolo = 2;
                //     longitud = 2;
                // } else if (operacionLinea.contains("*")) {
                //     simbolo = 3;
                //     longitud = 2;
                // } else if (operacionLinea.contains("/")) {
                //     simbolo = 4;
                //     longitud = 2;
                // } else if (operacionLinea.contains("%")) {
                //     simbolo = 5;
                //     longitud = 2;
                // } else {
                //     simbolo = 6;
                //     longitud = 1;
                // }


                switch (partes[1]) { // El operador se encuentra en la segunda posición
                    case "+":
                        simbolo=1;
                        longitud=2;
                        //result = Double.parseDouble(parts[0]) + Double.parseDouble(parts[2]);
                        break;
                    case "-":
                        simbolo=2;
                        longitud=2;
                        //result = Double.parseDouble(parts[0]) - Double.parseDouble(parts[2]);
                        break;
                    case "*":
                        simbolo=3;
                        longitud=2;
                        //result = Double.parseDouble(parts[0]) * Double.parseDouble(parts[2]);
                        break;
                    case "/":
                        simbolo=4;
                        longitud=2;
                        //result = Double.parseDouble(parts[0]) / Double.parseDouble(parts[2]);
                        break;
                    case "%":
                        simbolo=5;
                        longitud=2;
                        //result = Double.parseDouble(parts[0]) % Double.parseDouble(parts[2]);
                        break;
                    case "!":
                        simbolo=6;
                        longitud=1;
                        //result = factorial(Integer.parseInt(parts[0]));
                        break;
                    default:
                        System.out.println("Operador no válido.");
                        return;
                }

                // Crear mensaje TLV
                byte[] mensaje = new byte[2 + longitud];

                mensaje[0] = (byte) simbolo;
                mensaje[1] = (byte) longitud;
                mensaje[2] = (byte) operando1;

                if (longitud == 2) {
                    mensaje[3] = (byte) operando2;
                }

                // Enviar mensaje al servidor
                salida.write(mensaje);

                // Recibir respuesta del servidor
                byte[] respuesta = new byte[40];
                entrada.read(respuesta);

                //debugging
                // System.out.println("Mensaje recibido: "+Arrays.toString(respuesta));


                 // Validar respuesta
                 if (respuesta[0] != 10) {
                    System.err.println("Respuesta del servidor no valida");
                    continue;
                }

                int codigomensaje1 =respuesta[2];
                int longmensaje1= respuesta[3];
                int codigomensaje2= respuesta[3+longmensaje1+1];
                int longmensaje2= respuesta[1]-longmensaje1-2-2; // para averiguar la longitud del mensaje 2 longitud menos longitud menssaje1 menos las cabeceras de cada uno
               

                long acumulador = 0;

                String errorMsg="NULL";

                //Primer caso solo acumulador
                if(codigomensaje1==16 && codigomensaje2==0){
                    // Obtener valor del acumulador
                    
                    for (int i = 4; i < 12; i++) {
                        acumulador = (acumulador << 8) | (respuesta[i] & 0xFFL);
                    }
    
                }

                else if(codigomensaje1==16 && codigomensaje2==11){
                    //Segundo caso acumulador mas error
                    
                        // Obtener valor del acumulador
                    
                    for (int i = 4; i < 12; i++) {
                        acumulador = (acumulador << 8) | (respuesta[i] & 0xFFL);
                    }
            
                    byte[] byteserror=Arrays.copyOfRange(respuesta, 14, 14+longmensaje2); //empezamos en el 14 y se resta pq empezamos a leer en el 14  por las cabeceras del error
                    errorMsg=new String(byteserror,StandardCharsets.UTF_8);

                    }
                    //Tercer caso error mas acumulador
                    else if(codigomensaje1==11 && codigomensaje2==16){

                        byte[] byteserror=Arrays.copyOfRange(respuesta, 4, 4+longmensaje1); //empezamos en el 14 por las cabeceras del error
                        errorMsg=new String(byteserror,StandardCharsets.UTF_8);

                         // Obtener valor del acumulador
                    
                    for (int i = 4+longmensaje1+2; i <4+longmensaje1+2+8 ; i++) {
                        acumulador = (acumulador << 8) | (respuesta[i] & 0xFFL);
                    }

                    

                    }
                    
                

               

                
                
                // Mostrar valor del acumulador
                if(errorMsg!="NULL"){
                    System.out.println(errorMsg);
                }
                System.out.println("Valor del acumulador: " + acumulador);
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Han pasado los 15 segundos del TimeOut");
        }
    }
}
