package br.ufs.dcomp.collina;

import br.ufs.dcomp.collina.service.funcionarios.ContratoServiceTest;
import br.ufs.dcomp.collina.service.funcionarios.EscalaServiceTest;
import br.ufs.dcomp.collina.service.funcionarios.RHServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class StandaloneTestRunner {

    public static void main(String[] args) {
        List<Class<?>> testClasses = List.of(
                EscalaServiceTest.class,
                RHServiceTest.class,
                ContratoServiceTest.class
        );

        int total = 0;
        int passed = 0;
        int failed = 0;

        System.out.println("==================================================");
        System.out.println(" Executando Testes Unitários de Funcionários e Escalas");
        System.out.println("==================================================");

        for (Class<?> clazz : testClasses) {
            System.out.println("\n--- " + clazz.getSimpleName() + " ---");

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Test.class)) {
                    total++;
                    try {
                        Object instance = clazz.getDeclaredConstructor().newInstance();

                        // Executa mocks e @InjectMocks manualmente ou mockito se inicializado
                        org.mockito.MockitoAnnotations.openMocks(instance).close();

                        // Chama @BeforeEach
                        for (Method m : clazz.getDeclaredMethods()) {
                            if (m.isAnnotationPresent(BeforeEach.class)) {
                                m.setAccessible(true);
                                m.invoke(instance);
                            }
                        }

                        // Chama o método de teste
                        method.setAccessible(true);
                        method.invoke(instance);

                        passed++;
                        System.out.println("  [PASS] " + method.getName());
                    } catch (Throwable t) {
                        failed++;
                        System.err.println("  [FAIL] " + method.getName() + " -> " + (t.getCause() != null ? t.getCause().getMessage() : t.getMessage()));
                        if (t.getCause() != null) {
                            t.getCause().printStackTrace();
                        } else {
                            t.printStackTrace();
                        }
                    }
                }
            }
        }

        System.out.println("\n==================================================");
        System.out.printf(" Resumo: Total = %d | Passaram = %d | Falharam = %d%n", total, passed, failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
