write.table("", "Experiments/GetData/R/HV.Wilcoxon.tex",append=FALSE)
resultDirectory<-"Experiments/GetData/data"
latexHeader <- function() {
  write.table("\\documentclass{article}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("\\title{StandardStudy}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("\\usepackage{amssymb}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("\\author{A.J.Nebro}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("\\begin{document}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("\\maketitle", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("\\section{Tables}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("\\", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
}

latexTableHeader <- function(problem, tabularString, latexTableFirstLine) {
  write.table("\\begin{table}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("\\caption{", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table(problem, "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table(".HV.}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)

  write.table("\\label{Table:", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table(problem, "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table(".HV.}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)

  write.table("\\centering", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("\\begin{scriptsize}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("\\begin{tabular}{", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table(tabularString, "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table(latexTableFirstLine, "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("\\hline ", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
}

printTableLine <- function(indicator, algorithm1, algorithm2, i, j, problem) { 
  file1<-paste(resultDirectory, algorithm1, sep="/")
  file1<-paste(file1, problem, sep="/")
  file1<-paste(file1, indicator, sep="/")
  data1<-scan(file1)
  file2<-paste(resultDirectory, algorithm2, sep="/")
  file2<-paste(file2, problem, sep="/")
  file2<-paste(file2, indicator, sep="/")
  data2<-scan(file2)
  if (i == j) {
    write.table("--", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  }
  else if (i < j) {
    if (is.finite(wilcox.test(data1, data2)$p.value) & wilcox.test(data1, data2)$p.value <= 0.05) {
      if (median(data1) >= median(data2)) {
        write.table("$\\blacktriangle$", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
}
      else {
        write.table("$\\triangledown$", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
}
}
    else {
      write.table("$-$", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
}
  }
  else {
    write.table(" ", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  }
}

latexTableTail <- function() { 
  write.table("\\hline", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("\\end{tabular}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("\\end{scriptsize}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
  write.table("\\end{table}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
}

latexTail <- function() { 
  write.table("\\end{document}", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
}

### START OF SCRIPT 
# Constants
problemList <-c("Australian", "Vehicle", "Sonar", "Hillvalley", "Musk1") 
algorithmList <-c("oipMOEAD-FS-2", "oipMOEAD-FS-4", "oipMOEAD-FS-8", "aspMOEAD-FS-2", "aspMOEAD-FS-4", "aspMOEAD-FS-8", "rdpMOEAD-FS-2", "rdpMOEAD-FS-4", "rdpMOEAD-FS-8", "MOEAD-STAT") 
tabularString <-c("lccccccccc") 
latexTableFirstLine <-c("\\hline  & oipMOEAD-FS-4 & oipMOEAD-FS-8 & aspMOEAD-FS-2 & aspMOEAD-FS-4 & aspMOEAD-FS-8 & rdpMOEAD-FS-2 & rdpMOEAD-FS-4 & rdpMOEAD-FS-8 & MOEAD-STAT\\\\ ") 
indicator<-"HV"

 # Step 1.  Writes the latex header
latexHeader()
tabularString <-c("| l | p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm } | p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm } | p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm } | p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm } | p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm } | p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm } | p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm } | p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm } | p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm }p{0.15cm } | ") 

latexTableFirstLine <-c("\\hline \\multicolumn{1}{|c|}{} & \\multicolumn{5}{c|}{oipMOEAD-FS-4} & \\multicolumn{5}{c|}{oipMOEAD-FS-8} & \\multicolumn{5}{c|}{aspMOEAD-FS-2} & \\multicolumn{5}{c|}{aspMOEAD-FS-4} & \\multicolumn{5}{c|}{aspMOEAD-FS-8} & \\multicolumn{5}{c|}{rdpMOEAD-FS-2} & \\multicolumn{5}{c|}{rdpMOEAD-FS-4} & \\multicolumn{5}{c|}{rdpMOEAD-FS-8} & \\multicolumn{5}{c|}{MOEAD-STAT} \\\\") 

# Step 3. Problem loop 
latexTableHeader("Australian Vehicle Sonar Hillvalley Musk1 ", tabularString, latexTableFirstLine)

indx = 0
for (i in algorithmList) {
  if (i != "MOEAD-STAT") {
    write.table(i , "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
    write.table(" & ", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)

    jndx = 0
    for (j in algorithmList) {
      for (problem in problemList) {
        if (jndx != 0) {
          if (i != j) {
            printTableLine(indicator, i, j, indx, jndx, problem)
          }
          else {
            write.table("  ", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
          } 
          if (problem == "Musk1") {
            if (j == "MOEAD-STAT") {
              write.table(" \\\\ ", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
            } 
            else {
              write.table(" & ", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
            }
          }
     else {
    write.table("&", "Experiments/GetData/R/HV.Wilcoxon.tex", append=TRUE)
     }
        }
      }
      jndx = jndx + 1
}
    indx = indx + 1
  }
} # for algorithm

  latexTableTail()

#Step 3. Writes the end of latex file 
latexTail()

