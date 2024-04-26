import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class tcpmtser {
    public static void main(String[] args) throws IOException {

        // Validar argumentos
        if (args.length != 1) {
            System.out.println("Sintaxis incorrecta. Uso correcto: tcpmtser puerto");
            return;
        }
        System.out.println("Servidor activo");
        // Obtener puerto
        int puerto = Integer.parseInt(args[0]);

        // Crear ServerSocket
        ServerSocket serverSocket = new ServerSocket(puerto);

        // Bucle de atención a clientes
        while (true) {
            // Esperar conexión de un cliente
            Socket socket = serverSocket.accept();
            System.out.println("Cliente conectado desde " + socket.getInetAddress() + ":" + socket.getPort());

            // Crear y ejecutar un nuevo hilo para cada cliente
            new Thread(new ClientHandler(socket)).start();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private long acumulador;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.acumulador = 0;
        }

        @Override
        public void run() {
            try {
                // Crear streams de entrada y salida
                InputStream entrada = clientSocket.getInputStream();
                OutputStream salida = clientSocket.getOutputStream();

                // Bucle de comunicación con el cliente
                while (true) {
                    // Obtener operación y operandos
                    byte[] mensajeSinFiltrar = new byte[4];
                    entrada.read(mensajeSinFiltrar);
                    byte[] mensaje;
                    if (mensajeSinFiltrar[0] == 6) {
                        mensaje = new byte[3];
                        mensaje[0] = mensajeSinFiltrar[0];
                        mensaje[1] = mensajeSinFiltrar[1];
                        mensaje[2] = mensajeSinFiltrar[2];
                    } else {
                        mensaje = new byte[4];
                        mensaje = mensajeSinFiltrar;
                    }
                    int operacion = mensaje[0];
                    int longitud = mensaje[1];
                    int operando1 = mensaje[2];

                    int operando2 = (longitud == 2) ? mensaje[3] : 0;

                    // Realizar operación
                    int resultado = 0;
                    String errorMesg="NULL";
                    switch (operacion) {
                        case 1:
                            resultado = operando1 + operando2;
                            System.out.println("Operacion recibida: " + operando1 + "+" + operando2);
                            break;
                        case 2:
                            resultado = operando1 - operando2;
                            System.out.println("Operacion recibida: " + operando1 + "-" + operando2);
                            break;
                        case 3:
                            resultado = operando1 * operando2;
                            System.out.println("Operacion recibida: " + operando1 + "*" + operando2);
                            break;
                        case 4:
                            if(operando2==0){
                                errorMesg="No division por 0";
                                resultado=0;
                            }else{
                            resultado = operando1 / operando2;
                            System.out.println("Operacion recibida: " + operando1 + "/" + operando2);
                            }
                            break;
                            
                        case 5:
                        if(operando2==0){
                            errorMesg="No modulo por 0";
                            resultado=0;
                        }else{
                            resultado = operando1 % operando2;
                            System.out.println("Operacion recibida: " + operando1 + "%" + operando2);
                        }break;
                        case 6:
                        if(operando1<0){
                            errorMesg="No factorial negativo";
                            resultado=0;
                        }else{
                            resultado = factorial(operando1);
                            System.out.println("Operacion recibida: " + operando1 + "!");
                         } break;
                    }
                    System.out.println("Resultado operacion: " + resultado);

                    // Actualizar acumulador
                    acumulador += resultado;
                    System.out.println("Valor acumulador: " + acumulador);

                    byte[] bufferAux = ByteBuffer.allocate(Long.BYTES).putLong(acumulador).array();//Los bytes del acumulador
                    

                    int L;
                    if(errorMesg=="NULL"){
                     L=10+2;//10 para el menssaje acumulador y 2 para cabecera principal
                    }else{
                        L=2+10+2+errorMesg.length();//2 para cabecera principal 10 mensj acumaldor 2 cabecera error y lo otro longitud error
                    }
                    // Crear mensaje de respuesta
                    byte[] respuesta = new byte[L];
                    respuesta[0] = 10;
                    respuesta[1]=(byte)(L-2);
                    //Primero acumulador
                    respuesta[2]=16;
                    respuesta[3]=8;
                    System.arraycopy(bufferAux, 0, respuesta, 4, 8);//copiar el acumulador en respuesta
                    if(errorMesg!="NULL"){
                    respuesta[12]=11;
                    respuesta[13]=(byte)errorMesg.length();
                    byte[] bufferError= errorMesg.getBytes(StandardCharsets.UTF_8);//los bytes del error con caracteres de ESPAÑA
                    System.arraycopy(bufferError, 0, respuesta, 14, respuesta[13]);
                 }
                    
                    // int codigomensaje1 =respuesta[2];
                    // int longmensaje1= respuesta[3];
                    // int codigomensaje2= respuesta[3+longmensaje1+1];
                    // int longmensaje2= respuesta[codigomensaje2+1];
                    // int longglobal= longmensaje1 + 2 + longmensaje2 + 2;





                    
                    

                    // Enviar respuesta al cliente
                    salida.write(respuesta);
                }
            } catch (IOException e) {
                System.err.println("Cliente se ha desconectado");
            } finally {
                // Cerrar socket del cliente
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("IOException: " + e.getMessage());
                }
            }
        }

        private int factorial(int n) {
            if (n == 0) {
                return 1;
            } else {
                return n * factorial(n - 1);
            }
        }
    }
}
