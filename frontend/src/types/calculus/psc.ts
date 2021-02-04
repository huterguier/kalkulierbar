import { RuleSet } from "./rules";
import { PropCalculusType, Calculus, FOCalculusType, PSCCalculusType } from ".";
import { KStringMap } from "../kotlin";

export interface PSCNode {
    type: string;
    parent: number | null;
    children: number[];
    leftFormulas: FormulaNode[];
    rightFormulas: FormulaNode[];
    isClosed: boolean;
    lastMove: PSCMove | null;
}

export interface FormulaNode {
    type: string;
    varName: string | null;
    child: FormulaNode | null;
    leftChild: FormulaNode | null;
    rightChild: FormulaNode | null;
    spelling: string | null;
    arguments: FormulaNode[] | null;
}

export type PSCTreeLayoutNode = PSCNode & { id: number};

export type FormulaTreeLayoutNode = FormulaNode & { id: string};

export type VarAssign = KStringMap<string>;

export interface PSCState {
    tree: PSCNode[];
    moveHistory: PSCMove[];
}
export interface FOSCState{
    tree: PSCNode[];
    moveHistory: PSCMove[];
}

export function instanceOfPSCState(
    object: any,
    calculus: PSCCalculusType,
): object is PSCState {
    return "ruleSet" in object && calculus === Calculus.psc;
}
export function instanceOfFOSCState(
    object: any,
    calculus: PSCCalculusType,
): object is FOSCState{
    return "tree" in object && calculus === Calculus.fosc;
}

export type PSCMove =
    PSCAxMove | PSCRuleMove | PSCUndoMove | PSCPruneMove;

export type FOSCMove = PSCMove | SCCloseAssignMove;

export interface SCCloseAssignMove{
    type:"sc-close-assign";
    id1: number;
    id2: number;
    varAssign: VarAssign;
}
export interface PSCRuleMove {
    type: string
    nodeID: number;
    listIndex: number;
    swapVariable?: string;
}

export interface PSCAxMove {
    type: "Ax";
    nodeID: number;
}

export interface PSCUndoMove {
    type: "undo";
}

export interface PSCPruneMove {
    type: "prune"
    nodeID: number;
}

export enum PSCType {
    help = "HELP"
}

export interface PSCParams {
    help: boolean;
}