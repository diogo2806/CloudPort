package br.com.cloudport.runtime;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "br.com.cloudport",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaModulosTest {

    @ArchTest
    static final ArchRule MODULOS_NAO_FORMAM_CICLOS = slices()
            .matching("br.com.cloudport.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule MODULOS_NAO_DEPENDEM_DO_RUNTIME = noClasses()
            .that().resideInAnyPackage(
                    "br.com.cloudport.servicoautenticacao..",
                    "br.com.cloudport.servicocargageral..",
                    "br.com.cloudport.servicogate..",
                    "br.com.cloudport.servicorail..",
                    "br.com.cloudport.servicoyard..",
                    "br.com.cloudport.serviconavio..",
                    "br.com.cloudport.serviconaviosiderurgico..",
                    "br.com.cloudport.visibilidade..")
            .should().dependOnClassesThat().resideInAPackage("br.com.cloudport.runtime..");

    @ArchTest
    static final ArchRule NAVIO_NAO_ACESSA_IMPLEMENTACAO_DO_YARD = noClasses()
            .that().resideInAnyPackage(
                    "br.com.cloudport.serviconavio..",
                    "br.com.cloudport.serviconaviosiderurgico..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.cloudport.servicoyard..repositorio..",
                    "br.com.cloudport.servicoyard..modelo..",
                    "br.com.cloudport.servicoyard..entidade..");
}
