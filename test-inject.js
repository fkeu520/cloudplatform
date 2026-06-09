// Test the regex-based injectFlowableProps logic
const xml = '<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"><bpmn:process id="leave-approval"><bpmn:userTask id="deptApproval" name="部门审批"/><bpmn:userTask id="hrApproval" name="人事审批"/></bpmn:process></bpmn:definitions>';

const flowableData = new Map();
flowableData.set('deptApproval', { candidateUsers: '2,3', candidateScope: 'company' });
flowableData.set('hrApproval', { candidateUsers: '5', candidateScope: 'dept' });

let result = xml;
if (!/xmlns:flowable\s*=/.test(result)) {
  result = result.replace(/(<bpmn:definitions\b[^>]*?)(\s*>)/, '$1 xmlns:flowable="http://flowable.org/bpmn"$2');
}

flowableData.forEach((data, elementId) => {
  const taskRegex = new RegExp(`(<bpmn:userTask\\b[^>]*?\\bid="${elementId}"[^>]*?)(\\s*/?>)`, 'g');
  result = result.replace(taskRegex, (match, attrs, close) => {
    // attrs 已经包含 <bpmn:userTask 标签名，只需处理属性部分
    let newAttrs = attrs.replace(/\s+flowable:[a-zA-Z]+="[^"]*"/g, '');
    const injectAttrs = [];
    if (data.candidateUsers) injectAttrs.push('flowable:candidateUsers="' + data.candidateUsers + '"');
    if (data.candidateScope) injectAttrs.push('flowable:candidateScope="' + data.candidateScope + '"');
    if (injectAttrs.length > 0) newAttrs += ' ' + injectAttrs.join(' ');
    return newAttrs + close;
  });
});

console.log('=== Result XML ===');
console.log(result);
console.log('=== Verification ===');
console.log('deptApproval has candidateUsers=2,3:', result.includes('id="deptApproval"') && result.includes('flowable:candidateUsers="2,3"'));
console.log('hrApproval has candidateUsers=5:', result.includes('id="hrApproval"') && result.includes('flowable:candidateUsers="5"'));
console.log('xmlns:flowable declared:', result.includes('xmlns:flowable='));
