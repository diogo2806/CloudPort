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
                "br.com.cloudport.serviconaviosiderurgico",
                "br.com.cloudport.servicoyard",
                "br.com.cloudport.servicogate",
                "br.com.cloudport.servicorail",
                "br.com.cloudport.servicoautenticacao",
                "br.com.cloudport.visibilidade"
        },
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaModulosTest {

    @ArchTest
    static final ArchRule MODULOS_NAO_DEVEM_FORMAR_CICLOS = slices()
            .matching("br.com.cloudport.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule MODULOS_NAO_DEPENDEM_DO_RUNTIME = noClasses()
            .that().resideOutsideOfPackage("br.com.cloudport.monolitonavio..")
            .should().dependOnClassesThat().resideInAPackage("br.com.cloudport.monolitonavio..");

    @ArchTest
    static final ArchRule RUNTIME_NAO_USA_ADAPTADORES_HTTP_INTERNOS = noClasses()
            .that().resideInAPackage("br.com.cloudport.monolitonavio..")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("HttpAdapter");

    @ArchTest
    static final ArchRule NAVIO_NAO_ACESSA_REPOSITORIO_EXTERNO = noClasses()
            .that().resideInAPackage("br.com.cloudport.serviconavio..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.cloudport.serviconaviosiderurgico..repositorio..",
                    "br.com.cloudport.servicoyard..repositorio..",
                    "br.com.cloudport.servicoyard..repository..",
                    "br.com.cloudport.servicogate..repository..",
                    "br.com.cloudport.servicorail..repositorio..",
                    "br.com.cloudport.servicoautenticacao..repositories..",
                    "br.com.cloudport.servicoautenticacao..usuarioslista..",
                    "br.com.cloudport.visibilidade..repository..");

    @ArchTest
    static final ArchRule SIDERURGICO_NAO_ACESSA_REPOSITORIO_EXTERNO = noClasses()
            .that().resideInAPackage("br.com.cloudport.serviconaviosiderurgico..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.cloudport.serviconavio..repositorio..",
                    "br.com.cloudport.serviconavio..repository..",
                    "br.com.cloudport.servicoyard..repositorio..",
                    "br.com.cloudport.servicoyard..repository..",
                    "br.com.cloudport.servicogate..repository..",
                    "br.com.cloudport.servicorail..repositorio..",
                    "br.com.cloudport.servicoautenticacao..repositories..",
                    "br.com.cloudport.visibilidade..repository..");

    @ArchTest
    static final ArchRule YARD_NAO_ACESSA_REPOSITORIO_EXTERNO = noClasses()
            .that().resideInAPackage("br.com.cloudport.servicoyard..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.cloudport.serviconavio..repositorio..",
                    "br.com.cloudport.serviconaviosiderurgico..repositorio..",
                    "br.com.cloudport.servicogate..repository..",
                    "br.com.cloudport.servicorail..repositorio..",
                    "br.com.cloudport.servicoautenticacao..repositories..",
                    "br.com.cloudport.visibilidade..repository..");

    @ArchTest
    static final ArchRule GATE_NAO_ACESSA_REPOSITORIO_EXTERNO = noClasses()
            .that().resideInAPackage("br.com.cloudport.servicogate..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.cloudport.serviconavio..repositorio..",
                    "br.com.cloudport.serviconaviosiderurgico..repositorio..",
                    "br.com.cloudport.servicoyard..repositorio..",
                    "br.com.cloudport.servicorail..repositorio..",
                    "br.com.cloudport.servicoautenticacao..repositories..",
                    "br.com.cloudport.visibilidade..repository..");

    @ArchTest
    static final ArchRule RAIL_NAO_ACESSA_REPOSITORIO_EXTERNO = noClasses()
            .that().resideInAPackage("br.com.cloudport.servicorail..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.cloudport.serviconavio..repositorio..",
                    "br.com.cloudport.serviconaviosiderurgico..repositorio..",
                    "br.com.cloudport.servicoyard..repositorio..",
                    "br.com.cloudport.servicogate..repository..",
                    "br.com.cloudport.servicoautenticacao..repositories..",
                    "br.com.cloudport.visibilidade..repository..");

    @ArchTest
    static final ArchRule AUTENTICACAO_NAO_ACESSA_REPOSITORIO_EXTERNO = noClasses()
            .that().resideInAPackage("br.com.cloudport.servicoautenticacao..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.cloudport.serviconavio..repositorio..",
                    "br.com.cloudport.serviconaviosiderurgico..repositorio..",
                    "br.com.cloudport.servicoyard..repositorio..",
                    "br.com.cloudport.servicogate..repository..",
                    "br.com.cloudport.servicorail..repositorio..",
                    "br.com.cloudport.visibilidade..repository..");

    @ArchTest
    static final ArchRule VISIBILIDADE_NAO_ACESSA_REPOSITORIO_EXTERNO = noClasses()
            .that().resideInAPackage("br.com.cloudport.visibilidade..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.cloudport.serviconavio..repositorio..",
                    "br.com.cloudport.serviconaviosiderurgico..repositorio..",
                    "br.com.cloudport.servicoyard..repositorio..",
                    "br.com.cloudport.servicogate..repository..",
                    "br.com.cloudport.servicorail..repositorio..",
                    "br.com.cloudport.servicoautenticacao..repositories..");
}
