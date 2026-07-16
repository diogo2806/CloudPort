package br.com.cloudport.monolitonavio;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = {
                "br.com.cloudport.monolitonavio",
                "br.com.cloudport.serviconavio",
                "br.com.cloudport.serviconaviosiderurgico"
        },
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaModulosTest {

    @ArchTest
    static final ArchRule MODULOS_NAO_DEVEM_FORMAR_CICLOS = slices()
            .matching("br.com.cloudport.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule NAVIO_NAO_DEPENDE_DO_SIDERURGICO = noClasses()
            .that().resideInAPackage("br.com.cloudport.serviconavio..")
            .should().dependOnClassesThat().resideInAPackage("br.com.cloudport.serviconaviosiderurgico..");

    @ArchTest
    static final ArchRule SIDERURGICO_NAO_DEPENDE_DA_IMPLEMENTACAO_DE_NAVIO = noClasses()
            .that().resideInAPackage("br.com.cloudport.serviconaviosiderurgico..")
            .should().dependOnClassesThat().resideInAPackage("br.com.cloudport.serviconavio..");

    @ArchTest
    static final ArchRule RUNTIME_NAO_USA_CLIENTE_HTTP_ENTRE_MODULOS_INCORPORADOS = noClasses()
            .that().resideInAPackage("br.com.cloudport.monolitonavio..")
            .should().dependOnClassesThat()
            .resideInAPackage("br.com.cloudport.serviconaviosiderurgico.cliente..");

    @ArchTest
    static final ArchRule NAVIO_NAO_ACESSA_REPOSITORIO_OU_DOMINIO_SIDERURGICO = noClasses()
            .that().resideInAPackage("br.com.cloudport.serviconavio..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.cloudport.serviconaviosiderurgico.repositorio..",
                    "br.com.cloudport.serviconaviosiderurgico.dominio..");

    @ArchTest
    static final ArchRule SIDERURGICO_NAO_ACESSA_REPOSITORIO_OU_DOMINIO_DE_NAVIO = noClasses()
            .that().resideInAPackage("br.com.cloudport.serviconaviosiderurgico..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.cloudport.serviconavio..repositorio..",
                    "br.com.cloudport.serviconavio..dominio..",
                    "br.com.cloudport.serviconavio..entidade..");
}
