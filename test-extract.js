// Test extractFlowableDataFromXml
const xml = '<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn"><bpmn:process id="leave-approval"><bpmn:userTask id="deptApproval" name="部门审批" flowable:candidateUsers="2,3" flowable:candidateScope="company"/><bpmn:userTask id="hrApproval" name="人事审批" flowable:candidateUsers="5"/><bpmn:sequenceFlow id="flow2" sourceRef="deptApproval" targetRef="hrApproval"><bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${day > 3}</bpmn:conditionExpression></bpmn:sequenceFlow></bpmn:process></bpmn:definitions>';

const flowableData = new Map();

// 模拟 extractFlowableDataFromXml
const elementRegex = /<bpmn:(\w+)\b([^>]*?)\/?>/g;
let match;
while ((match = elementRegex.exec(xml)) !== null) {
  const tagName = match[1];
  const attrsStr = match[2];
  const idMatch = attrsStr.match(/\bid="([^"]+)"/);
  if (!idMatch) continue;
  const id = idMatch[1];
  const data = {};

  const flowableAttrRegex = /\bflowable:([a-zA-Z]+)="([^"]*)"/g;
  let attrMatch;
  while ((attrMatch = flowableAttrRegex.exec(attrsStr)) !== null) {
    data[attrMatch[1]] = attrMatch[2];
  }

  if (tagName === 'sequenceFlow') {
    const condMatch = xml.match(
      new RegExp(`<bpmn:sequenceFlow\\b[^>]*?\\bid="${id}"[^>]*>[\\s\\S]*?<bpmn:conditionExpression[^>]*>([\\s\\S]*?)<\\/bpmn:conditionExpression>`)
    );
    if (condMatch) data.conditionExpression = condMatch[1];
  }

  if (Object.keys(data).length > 0) flowableData.set(id, data);
}

console.log('=== Extracted flowableData ===');
for (const [key, value] of flowableData) {
  console.log(key, '=>', JSON.stringify(value));
}

console.log('\n=== Verification ===');
const deptData = flowableData.get('deptApproval');
console.log('deptApproval.candidateUsers =', deptData?.candidateUsers, '(expected: 2,3)');
console.log('deptApproval.candidateScope =', deptData?.candidateScope, '(expected: company)');
const hrData = flowableData.get('hrApproval');
console.log('hrApproval.candidateUsers =', hrData?.candidateUsers, '(expected: 5)');
const flowData = flowableData.get('flow2');
console.log('flow2.conditionExpression =', flowData?.conditionExpression, '(expected: ${day > 3})');
